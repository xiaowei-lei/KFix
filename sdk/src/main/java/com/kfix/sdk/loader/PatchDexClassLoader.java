package com.kfix.sdk.loader;

import dalvik.system.BaseDexClassLoader;
import java.io.File;
import java.util.HashSet;
import java.util.Set;

public class PatchDexClassLoader extends BaseDexClassLoader {
    private final Set<String> shouldSkipPatchDexClassLoaderClassSet = new HashSet<>();
    private final Set<String> patchClassSet;
    private final ClassLoader appClassLoader;

    public PatchDexClassLoader(
            String dexPath,
            File optimizedDirectory,
            String librarySearchPath,
            Set<String> patchClassSet,
            ClassLoader appClassLoader,
            ClassLoader originalAppHostParentClassLoader
            ) {
        super(dexPath, optimizedDirectory, librarySearchPath, originalAppHostParentClassLoader);
        this.patchClassSet = patchClassSet;
        this.appClassLoader = appClassLoader;
    }

    @Override
    protected Class<?> loadClass(String className, boolean resolve) throws ClassNotFoundException {
        Class<?> c = findLoadedClass(className);
        if (c != null) {
            return c;
        }
        if (shouldSkipPatchDexClassLoaderClassSet.contains(className)) {
            return getParent().loadClass(className);
        } else {
            if (patchClassSet.contains(className)) {
                return super.loadClass(className, resolve);
            } else {
                shouldSkipPatchDexClassLoaderClassSet.add(className);
                Class<?> hostAppLoaded = appClassLoader.loadClass(className);
                shouldSkipPatchDexClassLoaderClassSet.remove(className);
                return hostAppLoaded;
            }
        }
    }
}
