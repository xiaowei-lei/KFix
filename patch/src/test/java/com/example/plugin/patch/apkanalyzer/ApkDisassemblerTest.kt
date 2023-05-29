package com.example.plugin.patch.apkanalyzer

import com.android.tools.proguard.ProguardMap
import com.kfix.patch.apkanalyzer.ApkDisassembler
import io.kotest.assertions.assertionCounter
import io.kotest.assertions.collectOrThrow
import io.kotest.assertions.eq.eq
import io.kotest.assertions.errorCollector
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import java.io.File
import org.junit.Test

class ApkDisassemblerTest {

    private val apkDisassembler = ApkDisassembler(
        apkFile = File("src/test/resources/apks/app-v1.apk"),
        proguardMap = ProguardMap().apply {
            readFromFile(File("src/test/resources/apks/mapping-v1.txt"))
        }
    )

    @Test
    fun `should disassemble correct result when obfuscated class exist in apk`() {
        val disassembleResult = apkDisassembler.disassemble("e2.c")
        disassembleResult.shouldNotBeNull()
        disassembleResult.obfuscatedClassName shouldBe "e2.c"
        val expectObfuscatedSmaliText = File("src/test/resources/apks/v1/e2.c.smali").readText()
        disassembleResult.obfuscatedSmaliText shouldBeIgnoreLine expectObfuscatedSmaliText

        disassembleResult.clearClassNameProvider() shouldBe "com.example.kotlinhotfix.demo.changed.ChangedClass"
        val expectClearSmaliText = File("src/test/resources/apks/v1/com.example.kotlinhotfix.demo.changed.ChangedClass.smali").readText()
        disassembleResult.clearSmaliTextProvider() shouldBeIgnoreLine expectClearSmaliText
    }

    private infix fun String.shouldBeIgnoreLine(expected: String) {
        fun String.filterLines(): List<String> {
            return lines().filter {
                !it.trim().startsWith(".line")
            }
        }
        val actual = this
        assertionCounter.inc()
        eq(actual.filterLines(), expected.filterLines())?.let(errorCollector::collectOrThrow)
    }

    @Test
    fun `should return null when class not exist in apk`() {
        apkDisassembler.disassemble("e2.c_xxxxx") shouldBe null
    }
}