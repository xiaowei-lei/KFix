package com.kfix.patch.generator

import com.android.tools.apk.analyzer.internal.SigUtils
import com.kfix.patch.apkanalyzer.ApkDisassembler
import com.kfix.patch.apkanalyzer.smaliText
import com.kfix.patch.log.Logger
import com.kfix.patch.generator.tools.ClassDependencyAnalyzer
import java.io.File
import org.jetbrains.kotlin.konan.properties.suffix

class PatchClassCollector(
    collectWorkspace: File,
    private val oldApkDisassembler: ApkDisassembler,
    private val newApkDisassembler: ApkDisassembler,
) {
    companion object {
        private const val SMALI = "smali"
        private const val TAG = "PatchClassCollector"
    }

    private val diffSmaliDir = File(collectWorkspace, "diff_smali")
    private val illegalAccessSmaliDir = File(collectWorkspace, "illegal_access_smali")
    private val patchSmaliDir = File(collectWorkspace, "patch_smali")

    init {
        listOf(diffSmaliDir, illegalAccessSmaliDir, patchSmaliDir).forEach {
            it.deleteRecursively()
            it.mkdirs()
        }
    }

    fun collect(): File {
        val contentChangedObfuscatedClassNames = collectClearContentChangedClasses()
        collectIllegalAccessClasses(contentChangedObfuscatedClassNames)
        return patchSmaliDir
    }

    private fun collectClearContentChangedClasses(): Set<String> {
        val subTag = "[collectClearContentChangedClasses]"
        return newApkDisassembler.asDisassembleSequence()
            .onEach {
                Logger.progress(TAG, "$subTag ${it.position}")
            }
            .filter(::writeSmaliIfContentChanged)
            .map { it.obfuscatedFullyQualifiedClassName }
            .toSet().also {
                Logger.progressEnd(TAG, "$subTag changedCount: ${it.size}")
            }
    }

    private fun collectIllegalAccessClasses(obfuscatedClassNames: Set<String>) {
        Logger.i(TAG, "[collectIllegalAccessClasses] start")
        val dependencyAnalyzer = ClassDependencyAnalyzer {
            newApkDisassembler.disassemble(SigUtils.signatureToName(it))?.obfuscatedClassDef
        }
        dependencyAnalyzer.analyzeDependencies(obfuscatedClassNames)
            .mapNotNull(newApkDisassembler::disassemble)
            .onEach {
                writePatchSmaliItem(
                    obfuscatedClassName = it.obfuscatedFullyQualifiedClassName,
                    obfuscatedSmaliText = it.obfuscatedClassDef.smaliText()
                )
                writeIllegalAccessSmaliItem(
                    clearClassName = it.clearFullyQualifiedClassName,
                    obfuscatedSmaliText = it.obfuscatedClassDef.smaliText(),
                    clearSmaliText = it.obfuscatedClassDef.smaliText()
                )
            }.also { illegalAccessClasses ->
                Logger.progressEnd(TAG, "[collectIllegalAccessClasses] found ${illegalAccessClasses.size} illegal access classes.")
            }
        Logger.i(TAG, "[collectIllegalAccessClasses] end")
    }

    private fun writeSmaliIfContentChanged(newItem: ApkDisassembler.DisassembleResult): Boolean {
        val newObfuscatedClassName = newItem.obfuscatedFullyQualifiedClassName
        val newClearClassName = newItem.clearFullyQualifiedClassName
        val newObfuscatedSmaliText = newItem.obfuscatedClassDef.smaliText()

        val oldItem = oldApkDisassembler.disassemble(newObfuscatedClassName)
        if (oldItem == null) {
            writePatchSmaliItem(
                obfuscatedClassName = newObfuscatedClassName,
                obfuscatedSmaliText = newObfuscatedSmaliText
            )
            writeDiffSmaliItem(
                newClearClassName = newClearClassName,
                newObfuscatedSmaliText = newObfuscatedSmaliText,
                newClearSmaliText = newItem.clearClassDef.smaliText()
            )
            return true
        } else {
            val newClearSmaliText = newItem.clearClassDef.smaliText()
            val oldClearSmaliText = oldItem.clearClassDef.smaliText()
            val oldObfuscatedSmaliText = oldItem.obfuscatedClassDef.smaliText()
            if (newObfuscatedSmaliText != oldObfuscatedSmaliText && !isSmaliContentSame(
                    newClearSmaliText,
                    oldClearSmaliText
                )
            ) {
                writePatchSmaliItem(
                    obfuscatedClassName = newObfuscatedClassName,
                    obfuscatedSmaliText = newObfuscatedSmaliText
                )
                writeDiffSmaliItem(
                    newClearClassName = newClearClassName,
                    newObfuscatedSmaliText = newObfuscatedSmaliText,
                    newClearSmaliText = newClearSmaliText,

                    oldObfuscatedSmaliText = oldObfuscatedSmaliText,
                    oldClearSmaliText = oldClearSmaliText
                )
                return true
            }
        }
        return false
    }

    private fun isSmaliContentSame(smali1: String, smali2: String): Boolean {
        fun linesOf(text: String): List<String> {
            return text.split("\n").filter { !it.trim().startsWith(".line") }
        }
        return linesOf(smali1) == linesOf(smali2)
    }

    private fun writePatchSmaliItem(
        obfuscatedClassName: String,
        obfuscatedSmaliText: String?,
    ) {
        write(
            parent = patchSmaliDir,
            name = obfuscatedClassName.suffix(SMALI),
            content = obfuscatedSmaliText
        )
    }

    private fun writeDiffSmaliItem(
        newClearClassName: String,
        newObfuscatedSmaliText: String,
        newClearSmaliText: String,
        oldObfuscatedSmaliText: String = "",
        oldClearSmaliText: String = "",
    ) {
        val clearClassNameFolderForDiff = File(diffSmaliDir, newClearClassName).also { it.mkdirs() }
        write(
            parent = clearClassNameFolderForDiff,
            name = "obfuscated_new".suffix(SMALI),
            content = newObfuscatedSmaliText
        )
        write(
            parent = clearClassNameFolderForDiff,
            name = "clear_new".suffix(SMALI),
            content = newClearSmaliText
        )
        write(
            parent = clearClassNameFolderForDiff,
            name = "obfuscated_old".suffix(SMALI),
            content = oldObfuscatedSmaliText
        )
        write(
            parent = clearClassNameFolderForDiff,
            name = "clear_old".suffix(SMALI),
            content = oldClearSmaliText
        )
    }

    private fun writeIllegalAccessSmaliItem(
        clearClassName: String,
        obfuscatedSmaliText: String,
        clearSmaliText: String,
    ) {
        val clearClassNameFolderForDiff =
            File(illegalAccessSmaliDir, clearClassName).also { it.mkdirs() }
        write(
            parent = clearClassNameFolderForDiff,
            name = "obfuscated".suffix(SMALI),
            content = obfuscatedSmaliText
        )
        write(
            parent = clearClassNameFolderForDiff,
            name = "clear".suffix(SMALI),
            content = clearSmaliText
        )
    }

    private fun write(
        parent: File,
        name: String,
        content: String?,
    ) {
        if (!content.isNullOrBlank()) {
            File(parent, name).writeText(content)
        }
    }
}