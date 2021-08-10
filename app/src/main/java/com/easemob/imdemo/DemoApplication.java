package com.easemob.imdemo;

import android.app.Application;

import com.hyphenate.util.EMLog;

public class DemoApplication extends Application {
    private static DemoApplication instance;
    private UserActivityLifecycleCallbacks mLifecycleCallbacks = new UserActivityLifecycleCallbacks();

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        registerActivityLifecycleCallbacks();
        initHx();
    }

    public static DemoApplication getInstance() {
        return instance;
    }

    private void registerActivityLifecycleCallbacks() {
        this.registerActivityLifecycleCallbacks(mLifecycleCallbacks);
    }

    public UserActivityLifecycleCallbacks getLifecycleCallbacks() {
        return mLifecycleCallbacks;
    }

    private void initHx() {
        // init hx sdk
        EMLog.i("DemoApplication", "application initHx");
        DemoHelper.getInstance().init(this);
    }

}
