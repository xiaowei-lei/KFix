package com.kfix.patch.generator.tools

import com.android.tools.apk.analyzer.internal.SigUtils
import com.android.tools.proguard.ProguardMap
import com.kfix.patch.apkanalyzer.ApkDisassembler
import com.kfix.patch.generator.tools.source.CaseAccessPackagePrivateClass
import com.kfix.patch.generator.tools.source.CaseAccessPublicClassPackagePrivateFieldOrMethod
import com.kfix.patch.generator.tools.source.CaseAccessPublicClassProtectedFieldOrMethod
import com.kfix.patch.generator.tools.source.CaseAccessPublicClassPublicFieldOrMethod
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import org.junit.Before
import org.junit.Test
import java.io.File
import java.util.Properties

class ClassDependencyAnalyzerTest {

    companion object {
        const val BUILD_TOOLS_VERSION = "33.0.0"
    }

    private val classesJava = File("build/classes/java/test")
    private val classesKotlin = File("build/classes/kotlin/test")

    private val mergedClasses = File("build/test/ClassReferenceFinderTest/classes")
    private val sourceZipFile = File("build/test/ClassReferenceFinderTest/source.zip")

    private lateinit var apkDisassembler: ApkDisassembler
    private lateinit var dependencyAnalyzer: ClassDependencyAnalyzer

    @Before
    fun setup() {
        classesToDex()
        apkDisassembler = ApkDisassembler(
            apkFile = sourceZipFile,
            proguardMap = ProguardMap()
        )
        dependencyAnalyzer = ClassDependencyAnalyzer {
            apkDisassembler.disassemble(SigUtils.signatureToName(it))?.obfuscatedClassDef
        }
    }

    @Test
    fun `should collect correctly for case patch class access package-private class`() {
        val startClassNames = listOf(
            CaseAccessPackagePrivateClass::class.java.name
        )
        dependencyAnalyzer.analyzeDependencies(startClassNames).sorted() shouldBe sortedSetOf(
            "com.kfix.patch.generator.tools.source.PackagePrivateClass2",
            "com.kfix.patch.generator.tools.source.PackagePrivateClass"
        )
    }

    @Test
    fun `should collect correctly for case patch class access public class's package-private field or method`() {
        val startClassNames = listOf(
            CaseAccessPublicClassPackagePrivateFieldOrMethod::class.java.name
        )
        dependencyAnalyzer.analyzeDependencies(startClassNames).sorted() shouldBe sortedSetOf(
            "com.kfix.patch.generator.tools.source.PublicClass"
        )
    }

    @Test
    fun `should collect correctly for case patch class access public class's public field or method`() {
        val startClassNames = listOf(
            CaseAccessPublicClassPublicFieldOrMethod::class.java.name
        )
        dependencyAnalyzer.analyzeDependencies(startClassNames).shouldBeEmpty()
    }

    @Test
    fun `should collect correctly for case patch class access public class's protected field or method`() {
        val startClassNames = listOf(
            CaseAccessPublicClassProtectedFieldOrMethod::class.java.name
        )
        dependencyAnalyzer.analyzeDependencies(startClassNames).shouldBeEmpty()
    }

    private fun getAndroidHome(): String {
        val properties = Properties().apply {
            kotlin.runCatching {
                load(File("../local.properties").inputStream())
            }
        }
        val androidHome = properties.getProperty("sdk.dir") ?: System.getenv("ANDROID_HOME")
        if (androidHome.isNullOrBlank()) {
            throw RuntimeException("Cannot find android home! Please config it on local.properties or config env path ANDROID_HOME.")
        }
        return androidHome
    }

    private fun d8(): String? {
        val d8 = File(
            getAndroidHome(),
            "build-tools${File.separator}$BUILD_TOOLS_VERSION${File.separator}d8"
        )
        require(d8.exists()) {
            "D8 file is not exist! Please make sure you've downloaded build tools version: $BUILD_TOOLS_VERSION"
        }
        return d8.absolutePath
    }

    private fun classesToDex() {
        mergedClasses.deleteRecursively()
        mergedClasses.mkdirs()
        sourceZipFile.delete()
        classesJava.copyRecursively(mergedClasses)
        classesKotlin.copyRecursively(mergedClasses)

        val classPaths = mergedClasses.walk()
            .filter { it.name.endsWith(".class") }
            .filter { it.path.contains("source") }
            .joinToString(separator = " ")
        val command = "${d8()} $classPaths --output ${sourceZipFile.absolutePath}"
        val process = Runtime.getRuntime().exec(command)
        process.waitFor()
        check(sourceZipFile.exists()) {
            "Generate ${sourceZipFile.name} failed, please checkout the command: `${command}`"
        }
    }
}