package com.kfix.patch.generator.collectors

import com.android.tools.proguard.ProguardMap
import com.android.tools.smali.dexlib2.AccessFlags
import com.kfix.patch.apkanalyzer.ApkDisassembler
import io.kotest.matchers.shouldBe
import java.io.File
import org.junit.Test

class IllegalAccessClassesCollectorTest {
    private val apkDisassembler = ApkDisassembler(
        apkFile = File("src/test/resources/apks/app-v2.apk"),
        proguardMap = ProguardMap().apply {
            readFromFile(File("src/test/resources/apks/mapping-v2.txt"))
        }
    )

    private val illegalAccessClassesCollector = IllegalAccessClassesCollector(apkDisassembler);

    @Test
    fun `should find all classes which illegal access to the host`() {
        apkDisassembler.asDisassembleSequence().forEach {
            val accessFlags = it.clearClassDef.accessFlags
            if (!AccessFlags.PUBLIC.isSet(accessFlags)) {
                println("Not public class: ${it.clearFullyQualifiedClassName} -> ${it.obfuscatedFullyQualifiedClassName}")
            }
        }
        val contentChangedItems = listOf(
            Collector.Item(
                className = "androidx.activity.ComponentActivity",
                collectorTag = "ModifiedClassesCollector"
            ),
        )
        illegalAccessClassesCollector.proceed(contentChangedItems) shouldBe mutableListOf<Collector.Item>().apply {
            val illegalAccessClasses = listOf(
                "androidx.activity.ComponentActivity\$3",
                "androidx.activity.ComponentActivity\$4",
                "androidx.activity.ComponentActivity\$5",
                "androidx.activity.ImmLeaksCleaner",
            ).map {
                Collector.Item(
                    className = it,
                    collectorTag = illegalAccessClassesCollector.tag()
                )
            }
            addAll(contentChangedItems)
            addAll(illegalAccessClasses)
        }
    }
}