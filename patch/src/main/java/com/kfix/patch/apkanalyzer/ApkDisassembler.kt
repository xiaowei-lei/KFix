package com.kfix.patch.apkanalyzer

import com.android.tools.apk.analyzer.dex.DexFiles
import com.android.tools.apk.analyzer.internal.SigUtils
import com.android.tools.proguard.ProguardMap
import com.example.plugin.patch.apkanalyzer.KFixDexDisassembler
import com.example.plugin.patch.apkanalyzer.smaliText
import java.io.File
import java.util.zip.ZipFile

class ApkDisassembler(
    apkFile: File,
    private val proguardMap: ProguardMap,
) {

    private val dexDisassemblerPairs = mutableListOf<DisassemblerPair>()

    data class DisassemblerPair(
        val clearDexDisassembler: KFixDexDisassembler,
        val obfuscatedDexDisassembler: KFixDexDisassembler,
    )

    init {
        ZipFile(apkFile.absolutePath).use { sourceZipFile ->
            sourceZipFile.entries().toList().filter {
                it.name.endsWith(".dex")
            }.forEach { entry ->
                val dexBytes = sourceZipFile.getInputStream(entry).readAllBytes()
                val dexFile = DexFiles.getDexFile(dexBytes)
                dexDisassemblerPairs.add(
                    DisassemblerPair(
                        clearDexDisassembler = KFixDexDisassembler(
                            dexFile = dexFile,
                            proguardMap = proguardMap
                        ),
                        obfuscatedDexDisassembler = KFixDexDisassembler(
                            dexFile = dexFile,
                            proguardMap = ProguardMap()
                        )
                    )
                )
            }
        }
    }

    private fun dexCount(): Int = dexDisassemblerPairs.size

    fun onEachClass(action: (Position, DisassembleResult) -> Unit) {
        dexDisassemblerPairs.forEachIndexed { dexIndex, pair ->
            pair.obfuscatedDexDisassembler.onEachClassDef { index, total, classDef ->
                val obfuscatedClassName = SigUtils.signatureToName(classDef.type)
                action(
                    Position(
                        dexIndex = dexIndex,
                        dexCount = dexCount(),
                        classCountInDex = total,
                        classIndexInDex = index
                    ),
                    DisassembleResult(
                        obfuscatedClassName = obfuscatedClassName,
                        obfuscatedSmaliText = classDef.smaliText(),
                        clearClassNameProvider = { clearClassNameOf(obfuscatedClassName) },
                        clearSmaliTextProvider = {
                            pair.clearDexDisassembler.disassembleClass(
                                clearClassNameOf(
                                    obfuscatedClassName
                                )
                            )
                        }
                    ))
            }
        }
    }

    data class Position(
        private val dexCount: Int,
        private val dexIndex: Int,
        private val classCountInDex: Int,
        private val classIndexInDex: Int,
    ) {
        val description =
            "Dex: ${dexIndex + 1}/${dexCount}, Class: ${classIndexInDex}/${classCountInDex}"
    }

    fun disassemble(obfuscatedClassName: String): DisassembleResult? {
        dexDisassemblerPairs.forEachIndexed { _, pair ->
            val obfuscatedSmaliText = kotlin.runCatching {
                pair.obfuscatedDexDisassembler.disassembleClass(obfuscatedClassName)
            }.getOrNull()
            if (obfuscatedSmaliText != null) {
                return DisassembleResult(
                    obfuscatedClassName = obfuscatedClassName,
                    obfuscatedSmaliText = obfuscatedSmaliText,
                    clearClassNameProvider = { clearClassNameOf(obfuscatedClassName) },
                    clearSmaliTextProvider = {
                        pair.clearDexDisassembler.disassembleClass(obfuscatedClassName)
                    }
                )
            }
        }
        return null
    }

    private fun clearClassNameOf(obfuscatedClassName: String): String {
        return proguardMap.getClassName(obfuscatedClassName)
    }

    data class DisassembleResult(
        val obfuscatedClassName: String,
        val obfuscatedSmaliText: String,
        val clearClassNameProvider: () -> String,
        val clearSmaliTextProvider: () -> String,
    )
}