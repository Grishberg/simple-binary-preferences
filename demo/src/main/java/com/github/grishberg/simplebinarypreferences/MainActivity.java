package com.github.grishberg.simplebinarypreferences;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import com.github.grishberg.binarypreferences.BinaryPreferences;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final String ALREADY_LAUNCHED = "launched";
    private static final String LAUNCH_COUNT = "count";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SharedPreferences preferences = new BinaryPreferences(this, "settings");
        boolean launched = preferences.getBoolean(ALREADY_LAUNCHED, false);
        int launchCount = preferences.getInt(LAUNCH_COUNT, 0);

        if (launched) {
            Log.e(TAG, "already launched " + launchCount + " times");
        } else {
            Log.e(TAG, "first time launched");
        }
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt(LAUNCH_COUNT, launchCount + 1);
        editor.putBoolean(ALREADY_LAUNCHED, true);
        editor.apply();
    }
}