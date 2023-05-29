package com.kfix.sample

import android.app.Application
import android.content.Context
import com.kfix.sdk.KFixRuntime
import com.kfix.sdk.Patch
import java.io.File
import kotlin.system.exitProcess

class SimpleApplication: Application() {
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            KFixRuntime().reset()
            SimplePatchManager.removePatch(base)
            exitProcess(1)
        }
        SimplePatchManager.getPatch(base)?.let { path ->
            val optimizedDirectory = File(cacheDir, File(path).nameWithoutExtension)
            optimizedDirectory.deleteRecursively()
            KFixRuntime().apply(
                Patch(
                    path = path,
                    oDexPath = optimizedDirectory.absolutePath,
                    libraryPath = null
                )
            )
            SimplePatchManager.removePatch(base)
        }
    }
}