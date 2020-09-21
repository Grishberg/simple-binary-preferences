package com.github.grishberg.simplebinarypreferences;

import android.app.Application;
import android.os.Debug;

public class App extends Application {
    private static final int BUFFER_IN_BYTES = 1024 * 1024 * 50;
    private static final int SAMPLING_IN_MICROSECONDS = 100;

    @Override
    public void onCreate() {
        if (BuildConfig.RECORD_TRACE_ON_START) {
            //Debug.startMethodTracingSampling("tracing.trace", BUFFER_IN_BYTES, SAMPLING_IN_MICROSECONDS);
            Debug.startMethodTracing("method_tracing.trace", BUFFER_IN_BYTES);
        }
        super.onCreate();
    }
}
