package com.kfix.sdk

import dalvik.system.BaseDexClassLoader
import java.io.File

internal class PatchDexClassLoader(
    dexPath: String,
    optimizedDirectory: File?,
    librarySearchPath: String?,
    originalAppHostParentClassLoader: ClassLoader,
    private val isPatchedClass: (String) -> Boolean,
    private val appClassLoader: ClassLoader,
) : BaseDexClassLoader(
    dexPath,
    optimizedDirectory,
    librarySearchPath,
    originalAppHostParentClassLoader
) {
    private val shouldSkipPatchDexClassLoaderClassSet = mutableSetOf<String>()

    override fun loadClass(className: String, resolve: Boolean): Class<*> {
        val c = findLoadedClass(className)
        if (c != null) {
            return c
        }
        return if (shouldSkipPatchDexClassLoaderClassSet.contains(className)) {
            parent.loadClass(className)
        } else {
            if (isPatchedClass(className)) {
                super.loadClass(className, resolve)
            } else {
                shouldSkipPatchDexClassLoaderClassSet.add(className)
                val hostAppLoaded = appClassLoader.loadClass(className)
                if (hostAppLoaded != null) {
                    shouldSkipPatchDexClassLoaderClassSet.remove(className)
                }
                hostAppLoaded
            }
        }
    }
}