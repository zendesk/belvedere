package com.zendesk.belvedere.ui;

import android.app.Activity;
import android.content.res.TypedArray;
import android.os.Build;
import android.support.annotation.AttrRes;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import static android.support.v4.content.ContextCompat.getColor;

/**
 * Set of util methods for dealing the with UI
 */
class UiUtils {

    /**
     * Hide the {@link Toolbar} of the specified {@link Activity}
     */
    static void hideToolbar(AppCompatActivity appCompatActivity) {
        if(appCompatActivity.getSupportActionBar() != null){
            appCompatActivity.getSupportActionBar().hide();
        }
    }

    /**
     * Dims out the StatusBar on the specified Activity when running
     * on API level {@link android.os.Build.VERSION_CODES#LOLLIPOP} and up.
     */
    static void dimStatusBar(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            final int transparent = ContextCompat.getColor(activity, android.R.color.transparent);
            activity.getWindow()
                    .setStatusBarColor(transparent);
        }
    }

    /**
     * Gets {@code colorAccent}.
     */
    static int getColorAccent(Activity activity) {
        return getColor(activity, getThemedResId(activity, R.attr.colorAccent));
    }

    private static int getThemedResId(Activity activity, @AttrRes int attr) {
        TypedArray a = activity.getTheme().obtainStyledAttributes(new int[]{attr});
        int resId = a.getResourceId(0, Integer.MIN_VALUE);
        a.recycle();

        if(resId != Integer.MIN_VALUE) {
            return resId;
        } else {
            return android.R.color.black;
        }
    }
}