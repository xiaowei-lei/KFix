package com.kfix.patch.generator

import com.android.tools.proguard.ProguardMap
import com.kfix.patch.apkanalyzer.ApkDisassembler
import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.matchers.shouldBe
import org.junit.Test
import java.io.File
import java.util.zip.ZipFile

class PatchGeneratorTest {
    private val patchGenerator = PatchGenerator(
        workspace = File("build/test/PatchGeneratorTest"),
    )

    @Test
    fun `should generate patch success`() {
        val patchFile = patchGenerator.generate(
            oldApkFile = File("src/test/resources/apks/app-v1.apk"),
            oldMappingFile = File("src/test/resources/apks/mapping-v1.txt"),
            newApkFile = File("src/test/resources/apks/app-v2.apk"),
            newMappingFile = File("src/test/resources/apks/mapping-v2.txt")
        )

        patchFile.exists() shouldBe true

        val expectChanged = listOf(
            "com.example.kotlinhotfix.PatchedActivity",
            "e2.g"
        )

        val zipFile = ZipFile(patchFile)
        zipFile.getInputStream(zipFile.getEntry("changed.txt")).readBytes()
            .toString(Charsets.UTF_8)
            .split("\n") shouldBe expectChanged

        ApkDisassembler(patchFile, ProguardMap())
            .asDisassembleSequence()
            .map { it.obfuscatedFullyQualifiedClassName }
            .toList()
            .sorted() shouldBe expectChanged
    }

    @Test
    fun `should throw exception when no changed class`() {
        shouldThrowExactly<PatchException> {
            patchGenerator.generate(
                oldApkFile = File("src/test/resources/apks/app-v1.apk"),
                oldMappingFile = File("src/test/resources/apks/mapping-v1.txt"),
                newApkFile = File("src/test/resources/apks/app-v1.apk"),
                newMappingFile = File("src/test/resources/apks/mapping-v1.txt"),
            )
        }
    }
}