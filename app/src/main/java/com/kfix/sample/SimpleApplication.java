package com.kfix.sample;

import android.app.Application;
import android.content.Context;
import com.kfix.sdk.KFixRuntime;
import com.kfix.sdk.Patch;
import java.io.File;

public class SimpleApplication extends Application {
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            new KFixRuntime().reset();
            e.printStackTrace();
            SimplePatchManager.removePatch(base);
            System.exit(1);
        });
        String patchPath = SimplePatchManager.getPatch(base);
        if (patchPath != null) {
            File optimizedDirectory = new File(new File(patchPath).getParentFile(), "oDex");
            KFixRuntime kFixRuntime = new KFixRuntime();
            kFixRuntime.apply(this, new Patch(patchPath, optimizedDirectory.getAbsolutePath(), null));
            SimplePatchManager.removePatch(base);
        }
    }
}