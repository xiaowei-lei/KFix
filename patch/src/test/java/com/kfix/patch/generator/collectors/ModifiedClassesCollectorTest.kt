package com.kfix.patch.generator.collectors

import com.android.tools.proguard.ProguardMap
import com.kfix.patch.apkanalyzer.ApkDisassembler
import io.kotest.matchers.shouldBe
import java.io.File
import org.junit.Test

class ModifiedClassesCollectorTest {

    private val modifiedClassesCollector = ModifiedClassesCollector(
        oldApkDisassembler = ApkDisassembler(
            apkFile = File("src/test/resources/apks/app-v1.apk"),
            proguardMap = ProguardMap().apply {
                readFromFile(File("src/test/resources/apks/mapping-v1.txt"))
            }
        ),
        newApkDisassembler = ApkDisassembler(
            apkFile = File("src/test/resources/apks/app-v2.apk"),
            proguardMap = ProguardMap().apply {
                readFromFile(File("src/test/resources/apks/mapping-v2.txt"))
            }
        )
    )
    @Test
    fun `should collect all clear content modified classes`() {
        modifiedClassesCollector.proceed(emptyList()) shouldBe listOf(
            Collector.Item(
                className = "e2.g",
                collectorTag = modifiedClassesCollector.tag()
            ),
            Collector.Item(
                className = "com.example.kotlinhotfix.PatchedActivity",
                collectorTag = modifiedClassesCollector.tag()
            )
        )
    }
}