package com.kfix.patch.apkanalyzer

import com.android.tools.apk.analyzer.dex.DexFiles
import com.android.tools.apk.analyzer.internal.SigUtils
import com.android.tools.proguard.ProguardMap
import com.android.tools.smali.dexlib2.iface.ClassDef
import java.io.File
import java.util.zip.ZipFile

class ApkDisassembler(
    apkFile: File,
    private val proguardMap: ProguardMap,
) {

    private val dexDisassemblerPairs: List<DisassemblerPair>

    data class DisassemblerPair(
        val clearDexDisassembler: KFixDexDisassembler,
        val obfuscatedDexDisassembler: KFixDexDisassembler,
    )

    init {
        ZipFile(apkFile.absolutePath).use { sourceZipFile ->
            dexDisassemblerPairs = sourceZipFile.entries().toList().filter {
                it.name.endsWith(".dex")
            }.map { entry ->
                val dexBytes = sourceZipFile.getInputStream(entry).readAllBytes()
                val dexFile = DexFiles.getDexFile(dexBytes)
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
            }
        }
    }

    private fun dexCount(): Int = dexDisassemblerPairs.size

    fun asDisassembleSequence(): Sequence<DisassembleResult> {
        return dexDisassemblerPairs.asSequence()
            .flatMapIndexed { dexIndex: Int, disassemblerPair: DisassemblerPair ->
                val obfuscatedDisassembler = disassemblerPair.obfuscatedDexDisassembler
                val clearDisassembler = disassemblerPair.clearDexDisassembler
                obfuscatedDisassembler.asClassDefSequence()
                    .mapIndexedNotNull { index: Int, obfuscatedClassDef: ClassDef ->
                        val obfuscatedClassName = SigUtils.signatureToName(obfuscatedClassDef.type)
                        val position =
                            "Dex: ${dexIndex + 1}/${dexCount()}, Class: ${index}/${obfuscatedDisassembler.classCount()}"
                        val clearClassDef =
                            clearDisassembler.disassembleClass(clearClassNameOf(obfuscatedClassName))
                        if (clearClassDef != null) {
                            DisassembleResult(
                                obfuscatedClassDef = obfuscatedClassDef,
                                clearClassDef = clearClassDef,
                                position = position

                            )
                        } else {
                            null
                        }
                    }
            }
    }

    fun disassemble(obfuscatedFullyQualifiedClassName: String): DisassembleResult? {
        return dexDisassemblerPairs.asSequence().mapNotNull { disassemblerPair: DisassemblerPair ->
            val obfuscatedClassDef = disassemblerPair.obfuscatedDexDisassembler.disassembleClass(
                obfuscatedFullyQualifiedClassName
            )
            if (obfuscatedClassDef != null) {
                val clearClassDef = disassemblerPair.clearDexDisassembler.disassembleClass(
                    clearClassNameOf(obfuscatedFullyQualifiedClassName)
                )
                if (clearClassDef != null) {
                    return@mapNotNull DisassembleResult(
                        obfuscatedClassDef = obfuscatedClassDef,
                        clearClassDef = clearClassDef
                    )
                }
            }
            return@mapNotNull null
        }.firstOrNull()
    }

    private fun clearClassNameOf(obfuscatedClassName: String): String {
        return proguardMap.getClassName(obfuscatedClassName)
    }

    data class DisassembleResult(
        val obfuscatedClassDef: ClassDef,
        val clearClassDef: ClassDef,
        val position: String? = null,
    ) {
        val obfuscatedFullyQualifiedClassName: String = SigUtils.signatureToName(obfuscatedClassDef.type)
        val clearFullyQualifiedClassName: String = SigUtils.signatureToName(clearClassDef.type)
    }
}