package ru.vmochalov.vkchart;

import android.app.Application;

import timber.log.Timber;

/**
 * Created by Vladimir Mochalov on 10.03.2019.
 */
public class ChartApplication extends Application {

    public void onCreate() {
        super.onCreate();

        Timber.plant(new Timber.DebugTree());
    }
}
