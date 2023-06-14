package com.kfix.patch

import com.kfix.patch.generator.PatchGenerator
import java.io.File
import kotlin.system.exitProcess

object Main {
    private const val WORKSPACE = "<workspace directory>"
    private const val OLD_APK = "<old apk file>"
    private const val OLD_MAPPING = "<old mapping file>"
    private const val NEW_APK = "<new apk file>"
    private const val NEW_MAPPING = "<new mapping file>"

    @JvmStatic
    fun main(args: Array<String>) {
        if (args.size < 5) {
            printHelpTips()
            exitProcess(1)
        }
        PatchGenerator(
            workspace = file(args[0], WORKSPACE)
        ).generate(
            oldApkFile = file(args[1], OLD_APK),
            oldMappingFile = file(args[2], OLD_MAPPING),
            newApkFile = file(args[3], NEW_APK),
            newMappingFile = file(args[4], NEW_MAPPING)
        )
    }

    private fun file(path: String, tag: String) = File(path).also {
        if (!it.exists()) {
            System.err.println("$tag not exist! Path: ${it.absolutePath}")
            printHelpTips()
        }
    }

    private fun printHelpTips() {
        val tips = """
                =======================================
                Invalid args, please check your args! 
                Generating patch command:
                ./gradlew :patch:run --args "$WORKSPACE $OLD_APK $OLD_MAPPING $NEW_APK $NEW_MAPPING"
                =======================================
            """.trimIndent()
        System.err.println(tips)
    }
}