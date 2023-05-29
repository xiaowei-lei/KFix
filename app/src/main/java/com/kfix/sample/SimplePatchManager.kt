package com.kfix.sample

import android.content.Context

object SimplePatchManager {
    private const val SP_FILE = "patch_${BuildConfig.VERSION_CODE}"
    private const val KEY_PATCH_FILE_PATH = "patch_file_path"
    fun savePatch(context: Context, patchFilePath: String) {
        context.getSharedPreferences(SP_FILE, Context.MODE_PRIVATE).edit().putString(
            KEY_PATCH_FILE_PATH, patchFilePath).apply()
    }

    fun getPatch(context: Context): String? {
        return context.getSharedPreferences(SP_FILE, Context.MODE_PRIVATE).getString(
            KEY_PATCH_FILE_PATH, "")
    }

    fun removePatch(context: Context) {
        context.getSharedPreferences(SP_FILE, Context.MODE_PRIVATE).edit().remove(
            KEY_PATCH_FILE_PATH
        ).apply()
    }
}