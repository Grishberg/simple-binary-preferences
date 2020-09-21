package com.github.grishberg.simplebinarypreferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Debug;
import android.util.Log;

import com.github.grishberg.binarypreferences.BinaryPreferences;

import java.util.HashSet;
import java.util.Set;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final String PERF_TAG = "PROFILING";

    private static final String ALREADY_LAUNCHED_KEY = "launched";
    private static final String LAUNCH_COUNT_KEY = "count";
    private static final String STRING_KEY = "string_key";
    private static final String MULTIPLE_LONG_KEY_PREFIX = "multiple_long_";
    private static final double NANOS = 1000000.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        boolean launched1 = false;
        boolean launched2 = false;
        int launchCount1 = 0;
        int launchCount2 = 0;
        String storedValue1;
        String storedValue2;

        long start;

        Set<String> someSet = new HashSet<>();
        someSet.add("String1");
        someSet.add("String2");
        someSet.add("String3");
        someSet.add("String4");
        someSet.add("String5");
        someSet.add("String6");
        someSet.add("String7");
        someSet.add("String8");
        someSet.add("String9");
        someSet.add("String10");

        start = System.nanoTime();
        SharedPreferences preferences = new BinaryPreferences(this, "settings");
        launched2 = preferences.getBoolean(ALREADY_LAUNCHED_KEY, false);
        launchCount2 = preferences.getInt(LAUNCH_COUNT_KEY, 0);
        storedValue2 = preferences.getString(STRING_KEY, "");
        long duration = System.nanoTime() - start;

        start = System.nanoTime();
        SharedPreferences xmlPreferences = getSharedPreferences("xml_settings", Context.MODE_PRIVATE);
        launched1 = xmlPreferences.getBoolean(ALREADY_LAUNCHED_KEY, false);
        launchCount1 = xmlPreferences.getInt(LAUNCH_COUNT_KEY, 0);
        storedValue1 = xmlPreferences.getString(STRING_KEY, "");
        long xmlPreferencesDuration = System.nanoTime() - start;

        Log.e(TAG, "stored strings: " + storedValue1 + " - " + storedValue2);
        Log.e(PERF_TAG, "android : " + xmlPreferencesDuration / NANOS + " ms VS binary prefs: " + duration / NANOS + " ms");
        if (launched1 | launched2) {
            Log.e(TAG, "already launched " + launchCount1 + ":" + launchCount2 + " times");
        } else {
            Log.e(TAG, "first time launched");
        }
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt(LAUNCH_COUNT_KEY, launchCount1 + 1);
        editor.putBoolean(ALREADY_LAUNCHED_KEY, true);
        editor.putString(STRING_KEY, getPackageName());
        editor.putStringSet("STRING_SET_KEY", someSet);

        SharedPreferences.Editor editorXml = xmlPreferences.edit();
        editorXml.putInt(LAUNCH_COUNT_KEY, launchCount2 + 1);
        editorXml.putBoolean(ALREADY_LAUNCHED_KEY, true);
        editorXml.putString(STRING_KEY, getPackageName());
        editorXml.putStringSet("STRING_SET_KEY", someSet);

        for (int i = 0; i < 500; i++) {
            editor.putLong(MULTIPLE_LONG_KEY_PREFIX + i, i);
            editorXml.putLong(MULTIPLE_LONG_KEY_PREFIX + i, i);
        }

        for (int i = 0; i < 500; i++) {
            editor.putString("multiple_string_" + i, "some string value " + i);
            editorXml.putString("multiple_string_" + i, "some string value " + i);
        }

        for (int i = 0; i < 500; i++) {
            editor.putFloat("multiple_float_" + i, i);
            editorXml.putFloat("multiple_float_" + i, i);
        }

        editor.apply();
        editorXml.apply();

        if (BuildConfig.RECORD_TRACE_ON_START) {
            Debug.stopMethodTracing();
        }
    }
}