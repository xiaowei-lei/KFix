package com.kfix.sample.patch;

import android.app.Application;
import android.content.Context;
import android.content.res.Configuration;
import androidx.annotation.NonNull;
import com.kfix.sample.MyApplication;
import com.kfix.sdk.app.ApplicationLifeCycleBridge;
import com.kfix.sdk.KFixRuntime;
import com.kfix.sdk.Patch;
import java.io.File;

public class SimpleApplication extends Application {
    /**
     * Please use {@link MyApplication} to init your application.
     */
    private static final String REAL_APPLICATION = "com.kfix.sample.MyApplication";
    private final ApplicationLifeCycleBridge applicationBridge = new ApplicationLifeCycleBridge(this, REAL_APPLICATION);
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
        applicationBridge.attachBaseContext(base);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        applicationBridge.onCreate();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        applicationBridge.onLowMemory();
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        applicationBridge.onTrimMemory(level);
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        applicationBridge.onTerminate();
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        applicationBridge.onConfigurationChanged(newConfig);
    }
}