package com.kfix.sdk;

import android.app.Application;
import java.io.File;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class KFixRuntime {

    private static final String CHANGED_CLASSES_ENTRY_NAME = "changed.txt";

    public void apply(Application application, Patch patch) {
        try {
            ClassLoader originalAppClassLoader = KFixRuntime.class.getClassLoader();
            ClassLoader appClassLoaderParent = originalAppClassLoader != null ? originalAppClassLoader.getParent() : null;
            if (appClassLoaderParent != null) {
                Set<String> patchClassSet = patchedClassOf(patch);
                if (!patchClassSet.isEmpty()) {
                    ClassLoaderInjector.inject(application, appClassLoaderParent);
                    ClassLoader newHookedAppClassLoader = application.getClassLoader();
                    PatchDexClassLoader patchDexClassLoader = new PatchDexClassLoader(
                            patch.path,
                            new File(patch.oDexPath),
                            patch.libraryPath,
                            patchClassSet,
                            newHookedAppClassLoader,
                            newHookedAppClassLoader.getParent()
                    );
                    hackToParent(newHookedAppClassLoader, patchDexClassLoader);
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public void reset() {
        try {
            ClassLoader appClassLoader = KFixRuntime.class.getClassLoader();
            ClassLoader child = appClassLoader;
            ClassLoader tmpClassLoader = appClassLoader != null ? appClassLoader.getParent() : null;
            while (tmpClassLoader != null) {
                if (tmpClassLoader instanceof PatchDexClassLoader) {
                    hackToParent(child, tmpClassLoader.getParent());
                    break;
                }
                child = tmpClassLoader;
                tmpClassLoader = tmpClassLoader.getParent();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Set<String> patchedClassOf(Patch patch) {
        try (ZipFile zipFile = new ZipFile(patch.path)) {
            List<? extends ZipEntry> entries = Collections.list(zipFile.entries());
            for (ZipEntry entry : entries) {
                if (entry.getName().equals(CHANGED_CLASSES_ENTRY_NAME)) {
                    byte[] bytes = new byte[(int) entry.getSize()];
                    zipFile.getInputStream(entry).read(bytes);
                    String content = new String(bytes, StandardCharsets.UTF_8);
                    return new HashSet<>(Arrays.asList(content.split("\n")));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Collections.emptySet();
    }

    private void hackToParent(ClassLoader based, ClassLoader newParentClassLoader) throws Exception {
        Field parentField = parentField(based);
        if (parentField == null) {
            throw new RuntimeException("Not found parent field on ClassLoader!");
        }
        parentField.setAccessible(true);
        parentField.set(based, newParentClassLoader);
    }

    private Field parentField(ClassLoader classLoader) {
        ClassLoader parent = classLoader.getParent();
        Field field = null;
        for (Field f : ClassLoader.class.getDeclaredFields()) {
            try {
                boolean accessible = f.isAccessible();
                f.setAccessible(true);
                Object obj = f.get(classLoader);
                f.setAccessible(accessible);
                if (obj == parent) {
                    field = f;
                    break;
                }
            } catch (IllegalAccessException ignored) {
            }
        }
        return field;
    }
}
