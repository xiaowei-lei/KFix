package com.kfix.sdk.app;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Handler;
import android.os.Message;
import static com.kfix.sdk.app.ApplicationLifeCycleBridge.MSG_ATTACH_BASE_CONTEXT;
import static com.kfix.sdk.app.ApplicationLifeCycleBridge.MSG_ON_CONFIGURATION_CHANGED;
import static com.kfix.sdk.app.ApplicationLifeCycleBridge.MSG_ON_CREATE;
import static com.kfix.sdk.app.ApplicationLifeCycleBridge.MSG_ON_LOW_MEMORY;
import static com.kfix.sdk.app.ApplicationLifeCycleBridge.MSG_ON_TERMINATE;
import static com.kfix.sdk.app.ApplicationLifeCycleBridge.MSG_ON_TRIM_MEMORY;
import com.kfix.sdk.log.Logger;

public class ApplicationLifeCycleHandler extends Handler {
    private final ApplicationLifeCycle applicationLifeCycle;

    public ApplicationLifeCycleHandler(ApplicationLifeCycle applicationLifeCycle) {
        super();
        this.applicationLifeCycle = applicationLifeCycle;
        logClassLoaderName(getClass());
        if (applicationLifeCycle != null) {
            logClassLoaderName(applicationLifeCycle.getClass());
        }
    }

    private void logClassLoaderName(Class<?> clazz) {
        ClassLoader classLoader = clazz.getClassLoader();
        if (classLoader != null) {
            Logger.i("[ApplicationBarrierHandler]"+ clazz.getName() + "'s ClassLoader: " + classLoader.getClass().getName());
        }
    }

    @Override
    public void handleMessage(Message msg) {
        if (applicationLifeCycle == null) return;
        switch (msg.what) {
            case MSG_ATTACH_BASE_CONTEXT:
                applicationLifeCycle.attachBaseContext((Context) msg.obj);
                break;
            case MSG_ON_CREATE:
                applicationLifeCycle.onCreate();
                break;
            case MSG_ON_LOW_MEMORY:
                applicationLifeCycle.onLowMemory();
                break;
            case MSG_ON_TRIM_MEMORY:
                int level = (Integer) msg.obj;
                applicationLifeCycle.onTrimMemory(level);
                break;
            case MSG_ON_TERMINATE:
                applicationLifeCycle.onTerminate();
                break;
            case MSG_ON_CONFIGURATION_CHANGED:
                Configuration configuration = (Configuration) msg.obj;
                applicationLifeCycle.onConfigurationChanged(configuration);
                break;
            default:
                break;
        }
    }
}