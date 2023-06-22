package com.kfix.patch.generator.collectors

import com.kfix.patch.apkanalyzer.ApkDisassembler
import com.kfix.patch.apkanalyzer.smaliText
import com.kfix.patch.log.Logger

class ModifiedClassesCollector(
    private val oldApkDisassembler: ApkDisassembler,
    private val newApkDisassembler: ApkDisassembler,
) : Collector {

    override fun proceed(items: Collection<Collector.Item>): Collection<Collector.Item> {
        val obfuscatedClassNames = items.map { it.className }.toSet()
        val findItems = collectClearContentChangedClasses()
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

    private fun collectClearContentChangedClasses(): Set<String> {
        var changedCount = 0
        return newApkDisassembler.asDisassembleSequence()
            .mapNotNull { item ->
                val contentChanged = isContentChanged(item)
                if (contentChanged) {
                    changedCount++
                }
                Logger.progress(tag(), "${item.position} changedCount: $changedCount")
                if (contentChanged) item.obfuscatedFullyQualifiedClassName else null
            }
            .toSet()
            .also {
                Logger.progressEnd(tag(), "changedCount: $changedCount")
            }
    }

    private fun isContentChanged(newItem: ApkDisassembler.DisassembleResult): Boolean {
        val newObfuscatedClassName = newItem.obfuscatedFullyQualifiedClassName
        val oldItem = oldApkDisassembler.disassemble(newObfuscatedClassName)

        val newClearSmaliText = newItem.clearClassDef.smaliText()
        val oldClearSmaliText = oldItem?.clearClassDef?.smaliText()
        return !isSmaliContentSame(
            newClearSmaliText,
            oldClearSmaliText
        )
    }

    private fun isSmaliContentSame(smali1: String?, smali2: String?): Boolean {
        fun linesOf(text: String): List<String> {
            return text.split("\n").filter { !it.trim().startsWith(".line") }
        }
        return linesOf(smali1.orEmpty()) == linesOf(smali2.orEmpty())
    }
}