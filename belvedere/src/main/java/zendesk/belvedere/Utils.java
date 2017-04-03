package zendesk.belvedere;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.View;
import android.widget.ImageView;

import zendesk.belvedere.ui.R;

/**
 * Set of util methods for dealing with the UI and framework.
 */
class Utils {

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

    static void showToolbar(View view, boolean show) {
        final View toolbar = view.findViewById(R.id.image_stream_toolbar);
        toolbar.setVisibility(show ? View.VISIBLE : View.GONE);

        View toolbarContainer = view.findViewById(R.id.image_stream_toolbar_container);
        if(toolbarContainer != null) {
            toolbarContainer.setVisibility(show ? View.VISIBLE : View.GONE);
        }

    }

    static int getThemeColor(Context context, int attr){

        final TypedValue outValue = new TypedValue();
        final Resources.Theme theme = context.getTheme();

        final boolean wasResolved = theme.resolveAttribute(attr, outValue, true);

        if (wasResolved) {
            return outValue.resourceId == 0
                    ? outValue.data
                    : ContextCompat.getColor(context, outValue.resourceId);
        } else {
            return Color.BLACK;

        }
    }

    static void internalSetTint(ImageView imageView, int color) {
        if(imageView == null) {
            return;
        }

        Drawable d = DrawableCompat.wrap(imageView.getDrawable());
        if(d != null) {
            DrawableCompat.setTint(d.mutate(), color);
        }

        imageView.invalidate();
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

    /**
     * Checks whether an app is installed and enabled.
     *
     * @param packageName The package name of an app.
     * @param context The context.
     * @return true if app is installed and enabled, false otherwise.
     */
    static boolean isAppAvailable(String packageName, Context context) {
        try {
            return context.getPackageManager().getApplicationInfo(packageName, PackageManager.GET_META_DATA).enabled;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }
}
