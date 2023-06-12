package com.kfix.patch.generator

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
        val zipFile = ZipFile(patchFile)
        val classNames = zipFile.getInputStream(zipFile.getEntry("changed.txt")).readBytes()
            .toString(Charsets.UTF_8).split("\n")
        classNames.sorted() shouldBe listOf(
            "e2.g",
            "com.example.kotlinhotfix.PatchedActivity"
        ).sorted()
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