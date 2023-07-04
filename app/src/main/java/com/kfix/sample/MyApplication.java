package com.kfix.sample;

import android.app.Application;
import android.content.Context;
import android.content.res.Configuration;
import androidx.annotation.Keep;
import com.kfix.sdk.app.ApplicationLifeCycle;

@Keep
public class MyApplication implements ApplicationLifeCycle {

    private final Application application;
    public MyApplication(Application application) {
        this.application = application;
    }
    
    @Override
    public void attachBaseContext(Context base) {
        // Init
    }

    @Override
    public void onCreate() {

    }

    @Override
    public void onLowMemory() {
        
    }

    @Override
    public void onTrimMemory(int level) {

    }

    @Override
    public void onTerminate() {

    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {

    }
}
