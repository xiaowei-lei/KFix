package com.kfix.sdk.app;

import android.app.Application;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Handler;
import android.os.Message;
import com.kfix.sdk.loader.ClassLoaderInjector;
import com.kfix.sdk.log.Logger;

public class ApplicationLifeCycleBridge {
    static final int MSG_ATTACH_BASE_CONTEXT = 1;
    static final int MSG_ON_CREATE = 2;
    static final int MSG_ON_LOW_MEMORY = 3;
    static final int MSG_ON_TRIM_MEMORY = 4;
    static final int MSG_ON_TERMINATE = 5;
    static final int MSG_ON_CONFIGURATION_CHANGED = 6;

    private final Application application;
    private final String applicationLifeCycleClassName;

    private Handler applicationLifeCycleHandler = null;

    public ApplicationLifeCycleBridge(Application application, String applicationLifeCycleClassName) {
        this.application = application;
        this.applicationLifeCycleClassName = applicationLifeCycleClassName;
    }

    public void attachBaseContext(Context base) {
        applicationLifeCycleHandler = createApplicationLifeCycleHandler();
        sendMessage(MSG_ATTACH_BASE_CONTEXT, base);
    }

    public void onCreate() {
        sendMessage(MSG_ON_CREATE);
    }

    public void onLowMemory() {
        sendMessage(MSG_ON_LOW_MEMORY);
    }

    public void onTrimMemory(int level) {
        sendMessage(MSG_ON_TRIM_MEMORY, level);
    }

    public void onTerminate() {
        sendMessage(MSG_ON_TERMINATE);
    }

    public void onConfigurationChanged(Configuration newConfig) {
        sendMessage(MSG_ON_CONFIGURATION_CHANGED, newConfig);
    }

    private void sendMessage(int what) {
        sendMessage(what, null);
    }

    private void sendMessage(int what, Object obj) {
        if (applicationLifeCycleHandler == null) return;
        Message message = Message.obtain(applicationLifeCycleHandler, what);
        message.obj = obj;
        applicationLifeCycleHandler.sendMessage(message);
    }

    private Handler createApplicationLifeCycleHandler() {
        ClassLoader classLoader = application.getClassLoader();
        if (ClassLoaderInjector.KFixHookClassLoader.class.getName().equals(classLoader.getClass().getName())) {
            try {
                Class<?> applicationLifeCycleClass = Class.forName(applicationLifeCycleClassName, false, classLoader);
                Object applicationLifeCycle = applicationLifeCycleClass.getConstructor(Application.class).newInstance(application);

                String applicationBarrierHandlerClassName = "com.kfix.sdk.app.ApplicationLifeCycleHandler";
                Class<?> handlerClass = Class.forName(applicationBarrierHandlerClassName, false, classLoader);

                return (Handler) (handlerClass.getDeclaredConstructors()[0].newInstance(applicationLifeCycle));
            } catch (Throwable throwable) {
                Logger.e("createApplicationLifeCycleHandler error!", throwable);
            }
        }
        return new ApplicationLifeCycleHandler(null);
    }
}
