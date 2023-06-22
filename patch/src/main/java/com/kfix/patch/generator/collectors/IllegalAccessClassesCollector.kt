package com.kfix.patch.generator.collectors

import com.android.tools.apk.analyzer.internal.SigUtils
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.kfix.patch.apkanalyzer.ApkDisassembler
import com.kfix.patch.generator.tools.PackagePrivateClassDependencyAnalyzer

class IllegalAccessClassesCollector(
    private val apkDisassembler: ApkDisassembler,
) : Collector {
    private val dependencyAnalyzer = PackagePrivateClassDependencyAnalyzer(::getClassDef)

    override fun proceed(items: Collection<Collector.Item>): Collection<Collector.Item> {
        val obfuscatedClassNames = items.map { it.className }.toSet()
        val findItems = mutableSetOf<String>()
            .apply {
                addAll(obfuscatedClassNames)
                addAll(expandPatchClassesScopeForPatchAccessHost(obfuscatedClassNames))
            }
            .subtract(obfuscatedClassNames)
            .map {
                Collector.Item(
                    className = it,
                    collectorTag = tag()
                )
            }
        return mutableSetOf<Collector.Item>().apply {
            addAll(items)
            addAll(findItems)
        }
    }

    private fun getClassDef(classDefType: String): ClassDef? {
        return apkDisassembler.disassemble(SigUtils.signatureToName(classDefType))?.obfuscatedClassDef
    }

    private fun expandPatchClassesScopeForPatchAccessHost(patchClassNames: Collection<String>): Set<String> {
        return dependencyAnalyzer.analyzeDependencies(patchClassNames).toSet()
    }
}