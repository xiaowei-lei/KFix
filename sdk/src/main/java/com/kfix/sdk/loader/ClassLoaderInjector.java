package com.kfix.sdk.loader;

import android.app.Application;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.res.Resources;
import dalvik.system.PathClassLoader;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class ClassLoaderInjector {
    public static void inject(Application application, ClassLoader parentClassLoader) throws Throwable {
        // Cannot use PathClassLoader directly, will crash, but we can use DexClassLoader or Child class of PathClassLoader instead. todo: librarySearchPath
        ApplicationInfo applicationInfo = application.getApplicationInfo();
        ClassLoader pathClassLoader = new KFixHookClassLoader(applicationInfo.sourceDir, applicationInfo.nativeLibraryDir, parentClassLoader);
        replaceApplicationClassLoader(application, pathClassLoader);
    }

    public static class KFixHookClassLoader extends PathClassLoader {
        public KFixHookClassLoader(String dexPath, String librarySearchPath, ClassLoader parent) {
            super(dexPath, librarySearchPath, parent);
        }
    }

    /**
     * @see com.tencent.tinker.loader.NewClassLoaderInjector
     */
    private static void replaceApplicationClassLoader(Application application, ClassLoader newPathClassLoader) throws Throwable {
        Context baseContext = application.getBaseContext();
        try {
            getField(baseContext.getClass(), "mClassLoader").set(baseContext, newPathClassLoader);
        } catch (Throwable ignored) {
            // There's no mClassLoader field in ContextImpl before Android O.
            // However we should try our best to replace this field in case some
            // customized system has one.
        }

        Object basePackageInfo = getField(baseContext.getClass(), "mPackageInfo").get(baseContext);
        if (basePackageInfo != null) {
            getField(basePackageInfo.getClass(), "mClassLoader").set(basePackageInfo, newPathClassLoader);
        }

        Resources res = application.getResources();
        try {
            getField(res.getClass(), "mClassLoader").set(res, newPathClassLoader);
        } catch (Throwable ignored) {
            // Ignored.
        }

        try {
            Object drawableInflater = getField(res.getClass(), "mDrawableInflater").get(res);
            if (drawableInflater != null) {
                getField(drawableInflater.getClass(), "mClassLoader").set(drawableInflater, newPathClassLoader);
            }
        } catch (Throwable ignored) {
            // Ignored.
        }
    }

    private static Field getField(Class<?> clazz, String name) throws NoSuchFieldException {
        Class<?> currClazz = clazz;
        while (true) {
            try {
                Field field = currClazz.getDeclaredField(name);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException ignored) {
                currClazz = currClazz.getSuperclass();
                if (currClazz == null || currClazz == Object.class || Modifier.isAbstract(currClazz.getModifiers())) {
                    throw new NoSuchFieldException("Cannot find field " + name + " in class " + clazz.getName() + " and its super classes.");
                }
            }
        }
    }
}
