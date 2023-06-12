package com.kfix.patch.generator

import com.android.tools.apk.analyzer.dex.DexFiles
import com.android.tools.apk.analyzer.internal.SigUtils
import com.android.tools.smali.dexlib2.iface.DexFile
import com.android.tools.smali.smali.Main
import com.kfix.patch.apkanalyzer.smaliText
import com.kfix.patch.apkanalyzer.ApkDisassembler
import com.kfix.patch.log.Logger
import com.kfix.patch.proguard.PatchDexRewriter
import com.kfix.patch.proguard.PatchProguardMap
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.jetbrains.kotlin.konan.properties.suffix

class PatchGenerator(
    private val workspace: File,
) {
    companion object {
        private const val SMALI = "smali"
        private const val TAG = "PatchGenerator"
    }
    private val patchTmpDexFile = File(workspace, "patch_tmp.dex")

    private val patchRewriteObfuscatedSmaliDir = File(workspace, "patch_smali_write")
    private val patchDexFile = File(workspace, "classes.dex")
    private val changedClassFile = File(workspace, "changed.txt")

    private val patchZipFile = File(workspace, "patch.zip")

    fun generate(
        oldApkFile: File,
        oldMappingFile: File,
        newApkFile: File,
        newMappingFile: File,
    ): File {
        workspace.deleteRecursively()
        workspace.mkdirs()

        val oldProguardMap = PatchProguardMap.create(oldMappingFile)
        val newProguardMap = PatchProguardMap.create(newMappingFile)
        val patchClassDir = PatchClassCollector(
            collectWorkspace = File(workspace, "collect"),
            oldApkDisassembler = ApkDisassembler(
                apkFile = oldApkFile,
                proguardMap = oldProguardMap.proguardMap
            ),
            newApkDisassembler = ApkDisassembler(
                apkFile = newApkFile,
                proguardMap = newProguardMap
                    .proguardMap
            )
        ).collect()
        if (patchClassDir.walkTopDown().none { it.isFile && it.name.endsWith(SMALI) }) {
            throw PatchException("No changed classes.")
        }

        writeChangedFile(patchClassDir, changedClassFile)
        smaliToDex(patchClassDir, patchTmpDexFile)

        val rewriteDex = PatchDexRewriter().rewrite(
            newDexFile = DexFiles.getDexFile(patchTmpDexFile.readBytes()),
            oldProguardMap = oldProguardMap,
            newProguardMap = newProguardMap
        )
        patchTmpDexFile.delete()
        return outputPatch(rewriteDex).also {
            Logger.i(TAG, "The generated patch file: ${it.absolutePath}")
        }
    }

    private fun outputPatch(dexFile: DexFile): File {
        patchRewriteObfuscatedSmaliDir.deleteRecursively()
        patchRewriteObfuscatedSmaliDir.mkdirs()

        dexFile.classes.map {
            val className = SigUtils.signatureToName(it.type)
            File(patchRewriteObfuscatedSmaliDir, className.suffix(SMALI)).writeText(it.smaliText())
        }

        smaliToDex(patchRewriteObfuscatedSmaliDir, patchDexFile)
        changedClassFile.writeText(dexFile.classes.joinToString(separator = "\n") {
            SigUtils.signatureToName(
                it.type
            )
        })
        zip(listOf(patchDexFile, changedClassFile), patchZipFile)

        return patchZipFile
    }

    private fun writeChangedFile(patchObfuscatedSmaliDir: File, changedClassFile: File) {
        patchObfuscatedSmaliDir.walkTopDown().drop(1).filter { it.name.endsWith(SMALI) }
            .map { it.nameWithoutExtension }.let {
                changedClassFile.writeText(it.joinToString("\n"))
            }
    }

    private fun smaliToDex(smaliDir: File, outDexFile: File) {
        outDexFile.delete()
        outDexFile.createNewFile()
        outDexFile.apply {
            val command = "a ${smaliDir.path} -o $absolutePath"
            Main.main(command.split(" ").toTypedArray())
            if (!exists()) {
                Logger.i(TAG, "smali command: $command")
                throw PatchException("Generate patch failed.")
            }
        }
    }

    private fun zip(files: List<File>, destinationFile: File) {
        ZipOutputStream(FileOutputStream(destinationFile)).use { output ->
            files.forEach { file ->
                FileInputStream(file).use { input ->
                    val entry = ZipEntry(file.name)
                    output.putNextEntry(entry)
                    input.copyTo(output, 1024)
                }
            }
        }
    }
}

class PatchException(message: String, cause: Throwable? = null) : Throwable(message, cause)