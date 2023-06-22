package com.kfix.patch.generator

import com.android.tools.apk.analyzer.dex.DexFiles
import com.android.tools.apk.analyzer.internal.SigUtils
import com.android.tools.smali.dexlib2.iface.DexFile
import com.android.tools.smali.smali.Main
import com.kfix.patch.apkanalyzer.ApkDisassembler
import com.kfix.patch.apkanalyzer.smaliText
import com.kfix.patch.generator.collectors.Collector
import com.kfix.patch.generator.collectors.IllegalAccessClassesCollector
import com.kfix.patch.generator.collectors.ModifiedClassesCollector
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

    private val patchSmaliDir = File(workspace, "patch_smali")
    private val collectingDir = File(workspace, "collecting")

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
        val oldApkDisassembler = ApkDisassembler(oldApkFile, oldProguardMap.proguardMap)
        val newApkDisassembler = ApkDisassembler(newApkFile, newProguardMap.proguardMap)

        val patchItems = PatchClassCollectorChain(
            collectors = listOf(
                ModifiedClassesCollector(oldApkDisassembler, newApkDisassembler),
                IllegalAccessClassesCollector(newApkDisassembler)
            )
        ).proceed()
        if (patchItems.isEmpty()) {
            throw PatchException("No changed classes.")
        }

        writeForDebugging(patchItems, newApkDisassembler, oldApkDisassembler)
        writePatchSummary(patchItems)
        writePatchSmali(patchItems, newApkDisassembler)

        val patchTmpDexFile = File(workspace, "patch_tmp.dex").apply { deleteOnExit() }
        smaliToDex(patchSmaliDir, patchTmpDexFile)
        val rewriteDex = PatchDexRewriter().rewrite(
            newDexFile = DexFiles.getDexFile(patchTmpDexFile.readBytes()),
            oldProguardMap = oldProguardMap,
            newProguardMap = newProguardMap
        )

        return outputPatch(rewriteDex).also {
            Logger.i(TAG, "The generated patch file: ${it.absolutePath}")
        }
    }

    private fun writePatchSummary(patchItems: Set<Collector.Item>) {
        val summaryFile = File(workspace, "summary.txt")
        summaryFile.writeText(patchItems.joinToString(separator = "\n") { "[${it.collectorTag}] ${it.className}" })
    }

    private fun writePatchSmali(
        patchItems: Set<Collector.Item>,
        newApkDisassembler: ApkDisassembler,
    ) {
        patchSmaliDir.mkdirs()
        patchItems.mapNotNull { newApkDisassembler.disassemble(it.className) }.forEach {
            File(
                patchSmaliDir, it.obfuscatedFullyQualifiedClassName.suffix(SMALI)
            ).writeText(it.obfuscatedClassDef.smaliText())
        }
    }

    private fun writeForDebugging(
        patchItems: Set<Collector.Item>,
        newApkDisassembler: ApkDisassembler,
        oldApkDisassembler: ApkDisassembler,
    ) {
        for (item in patchItems) {
            val collectorTagDir = File(collectingDir, item.collectorTag)
            fun ApkDisassembler.DisassembleResult.write(prefix: String) {
                val clearClassNameDir = File(collectorTagDir, clearFullyQualifiedClassName).apply { mkdirs() }
                File(clearClassNameDir, "${prefix}_obfuscated.smali").writeText(obfuscatedClassDef.smaliText())
                File(clearClassNameDir, "${prefix}_clear.smali").writeText(clearClassDef.smaliText())
            }
            newApkDisassembler.disassemble(item.className)?.write("new")
            oldApkDisassembler.disassemble(item.className)?.write("old")
        }
    }

    private fun outputPatch(dexFile: DexFile): File {
        val patchRewriteObfuscatedSmaliDir = File(workspace, "patch_smali_write")
        val patchDexFile = File(workspace, "classes.dex")
        val changedFile = File(workspace, "changed.txt")

        patchRewriteObfuscatedSmaliDir.mkdirs()

        dexFile.classes.map {
            val className = SigUtils.signatureToName(it.type)
            File(patchRewriteObfuscatedSmaliDir, className.suffix(SMALI)).writeText(it.smaliText())
        }

        smaliToDex(patchRewriteObfuscatedSmaliDir, patchDexFile)

        val classNames = dexFile.classes.joinToString(separator = "\n") {
            SigUtils.signatureToName(it.type)
        }
        changedFile.writeText(classNames)

        val entries = listOf(patchDexFile, changedFile)
        zip(entries, patchZipFile)
        entries.forEach { it.delete() }
        return patchZipFile
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