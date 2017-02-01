package zendesk.belvedere;

import android.app.Activity;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;

import zendesk.belvedere.ui.R;

/**
 * Set of util methods for dealing the with UI
 */
class UiUtils {

    /**
     * Hide the {@link Toolbar} of the specified {@link Activity}
     */
    static void hideToolbar(AppCompatActivity appCompatActivity) {
        if(appCompatActivity.getSupportActionBar() != null) {
            appCompatActivity.getSupportActionBar().hide();
        }
        showToolbarContainer(appCompatActivity, false);
    }

    static void showToolbar(AppCompatActivity appCompatActivity) {
        if(appCompatActivity.getSupportActionBar() != null){
            appCompatActivity.getSupportActionBar().show();
        }
        showToolbarContainer(appCompatActivity, true);
    }

    private static void showToolbarContainer(Activity activity, boolean show) {
        View toolbarContainer = activity.findViewById(R.id.image_stream_toolbar_container);
        if(toolbarContainer != null) {
            toolbarContainer.setVisibility(show ? View.VISIBLE : View.GONE);
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
