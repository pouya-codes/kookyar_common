package com.kookyar.common;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Base splash screen activity with automatic version display and timed navigation.
 * Subclasses provide layout resource, target activity, and optional version view IDs.
 */
public abstract class BaseSplashActivity extends AppCompatActivity {

    private static final int DEFAULT_SPLASH_DURATION = 3000;
    private Handler handler;
    private Runnable splashRunnable;
    private boolean isNavigating = false;

    /** Return the layout resource for the splash screen. */
    protected abstract int getLayoutResource();

    /** Return the activity class to navigate to after splash. */
    protected abstract Class<?> getTargetActivity();

    /** Return splash duration in ms. Override to customize. */
    protected int getSplashDuration() {
        return DEFAULT_SPLASH_DURATION;
    }

    /** Return the resource ID for version name TextView, or 0 to skip. */
    protected int getVersionNameViewId() {
        return 0;
    }

    /** Return the resource ID for version code TextView, or 0 to skip. */
    protected int getVersionCodeViewId() {
        return 0;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getLayoutResource());

        displayVersionInfo();

        handler = new Handler(Looper.getMainLooper());
        splashRunnable = () -> {
            if (!isNavigating && !isFinishing() && !isDestroyed()) {
                isNavigating = true;
                Intent intent = new Intent(BaseSplashActivity.this, getTargetActivity());
                startActivity(intent);
                finish();
            }
        };
        handler.postDelayed(splashRunnable, getSplashDuration());
    }

    @Override
    protected void onDestroy() {
        isNavigating = true;
        if (handler != null && splashRunnable != null) {
            handler.removeCallbacks(splashRunnable);
        }
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (handler != null && splashRunnable != null) {
            handler.removeCallbacks(splashRunnable);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!isNavigating && handler != null && splashRunnable != null) {
            handler.postDelayed(splashRunnable, getSplashDuration());
        }
    }

    private void displayVersionInfo() {
        int nameId = getVersionNameViewId();
        int codeId = getVersionCodeViewId();
        if (nameId == 0 && codeId == 0) return;

        try {
            String versionName;
            long versionCode;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                PackageInfo info = getPackageManager().getPackageInfo(
                        getPackageName(), PackageManager.PackageInfoFlags.of(0));
                versionName = info.versionName;
                versionCode = info.getLongVersionCode();
            } else {
                @SuppressWarnings("deprecation")
                PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
                versionName = info.versionName;
                versionCode = info.versionCode;
            }

            if (nameId != 0) {
                TextView tv = findViewById(nameId);
                if (tv != null) tv.setText(versionName);
            }
            if (codeId != 0) {
                TextView tv = findViewById(codeId);
                if (tv != null) tv.setText(String.valueOf(versionCode));
            }
        } catch (PackageManager.NameNotFoundException ignored) {
        }
    }
}
