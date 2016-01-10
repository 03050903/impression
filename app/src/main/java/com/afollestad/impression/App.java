package com.afollestad.impression;

import android.app.Application;

import com.afollestad.inquiry.Inquiry;
import com.crashlytics.android.Crashlytics;
import com.squareup.leakcanary.LeakCanary;

import io.fabric.sdk.android.Fabric;

public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        if (!BuildConfig.DEBUG) {
            Fabric.with(this, new Crashlytics());
        }
        Inquiry.init(this, "impression", 1);

        LeakCanary.install(this);
    }
}