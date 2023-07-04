package com.kfix.sample.patch;

import android.content.Context;
import android.content.SharedPreferences;
import com.kfix.sample.BuildConfig;

public class SimplePatchManager {
    private static final String SP_FILE = "patch_" + BuildConfig.VERSION_CODE;
    private static final String KEY_PATCH_FILE_PATH = "patch_file_path";

    public static void savePatch(Context context, String patchFilePath) {
        SharedPreferences.Editor editor = context.getSharedPreferences(SP_FILE, Context.MODE_PRIVATE).edit();
        editor.putString(KEY_PATCH_FILE_PATH, patchFilePath);
        editor.apply();
    }

    public static String getPatch(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(SP_FILE, Context.MODE_PRIVATE);
        return preferences.getString(KEY_PATCH_FILE_PATH, null);
    }

    public static void removePatch(Context context) {
        SharedPreferences.Editor editor = context.getSharedPreferences(SP_FILE, Context.MODE_PRIVATE).edit();
        editor.remove(KEY_PATCH_FILE_PATH);
        editor.apply();
    }
}