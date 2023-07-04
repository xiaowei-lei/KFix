package com.kfix.sdk.app;

import android.content.Context;
import android.content.res.Configuration;

public interface ApplicationLifeCycle {

    void attachBaseContext(Context base);

    void onCreate();

    void onLowMemory();

    void onTrimMemory(int level);

    void onTerminate();

    void onConfigurationChanged(Configuration newConfig);
}
