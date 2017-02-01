package zendesk.belvedere;

import android.app.Activity;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

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

    static void showToolbar(AppCompatActivity appCompatActivity) {
        if(appCompatActivity.getSupportActionBar() != null){
            appCompatActivity.getSupportActionBar().show();
        }
    }

    /**
     * Dims out the StatusBar on the specified Activity when running
     * on API level {@link Build.VERSION_CODES#LOLLIPOP} and up.
     */
    static void dimStatusBar(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            final int transparent = ContextCompat.getColor(activity, android.R.color.transparent);
            activity.getWindow().setStatusBarColor(transparent);
        }
    }
}
