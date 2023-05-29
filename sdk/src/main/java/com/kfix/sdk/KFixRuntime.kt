package com.kfix.sdk

import java.io.File
import java.lang.reflect.Field
import java.util.zip.ZipFile

/**
 * 形成如下结构的classLoader树结构:
 * ---BootClassLoader
 * ----PatchDexClassLoader
 * ------PathClassLoader
 */
class KFixRuntime {

    companion object {
        private const val CHANGED_CLASSES_ENTRY_NAME = "changed.txt"
    }

    fun apply(patch: Patch) = kotlin.runCatching {
        val appClassLoader = KFixRuntime::class.java.classLoader
        val appClassLoaderParent = appClassLoader?.parent
        if (appClassLoader != null && appClassLoaderParent != null) {
            val patchedClasses = patchedClassOf(patch)
            if (patchedClasses.isNotEmpty()) {
                val hotfixClassLoader = PatchDexClassLoader(
                    dexPath = patch.path,
                    optimizedDirectory = File(patch.oDexPath),
                    librarySearchPath = patch.libraryPath,
                    isPatchedClass = { patchedClasses.contains(it) },
                    appClassLoader = appClassLoader,
                    originalAppHostParentClassLoader = appClassLoaderParent
                )
                hackToParent(
                    based = appClassLoader,
                    newParentClassLoader = hotfixClassLoader
                )
            }
        }
    }

    fun reset() = kotlin.runCatching {
        val appClassLoader = KFixRuntime::class.java.classLoader ?: return@runCatching
        var child: ClassLoader = appClassLoader
        var tmpClassLoader = appClassLoader.parent
        while (tmpClassLoader != null) {
            if (tmpClassLoader is PatchDexClassLoader) {
                hackToParent(
                    based = child,
                    newParentClassLoader = tmpClassLoader.parent
                )
                break
            }
            child = tmpClassLoader
            tmpClassLoader = tmpClassLoader.parent
        }
    }

    private fun patchedClassOf(patch: Patch): List<String> {
        return kotlin.runCatching {
            val zipFile = ZipFile(patch.path)
            zipFile.entries().toList().find { it.name == CHANGED_CLASSES_ENTRY_NAME }?.let {
                zipFile.getInputStream(it).readBytes().toString(Charsets.UTF_8).split("\n")
            }
        }.getOrNull() ?: listOf()
    }

    private fun hackToParent(based: ClassLoader, newParentClassLoader: ClassLoader) {
        val parentField = parentField(based) ?: throw RuntimeException("Not found parent field on ClassLoader!")
        parentField.isAccessible = true
        parentField.set(based, newParentClassLoader)
    }

    private fun parentField(classLoader: ClassLoader): Field? {
        val parent = classLoader.parent
        var field: Field? = null
        for (f in ClassLoader::class.java.declaredFields) {
            try {
                val accessible = f.isAccessible
                f.isAccessible = true
                val o = f[classLoader]
                f.isAccessible = accessible
                if (o === parent) {
                    field = f
                    break
                }
            } catch (ignore: IllegalAccessException) {
            }
        }
        return field
    }
}