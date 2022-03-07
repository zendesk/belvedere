package zendesk.belvedere;

import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.util.TypedValue;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.squareup.picasso.Transformation;
import java.util.Locale;
import zendesk.belvedere.ui.R;

/**
 * Set of util methods for dealing with the UI and framework.
 */
class Utils {

    static void showToolbar(View view, boolean show) {
        final View toolbar = view.findViewById(R.id.image_stream_toolbar);
        toolbar.setVisibility(show ? View.VISIBLE : View.GONE);

        View toolbarContainer = view.findViewById(R.id.image_stream_toolbar_container);
        if (toolbarContainer != null) {
            toolbarContainer.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    static void showBottomSheetDialog(View view, String message, long length, CharSequence actionMessage,
            View.OnClickListener onActionClickListener
    ) {
        final BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(view.getContext());
        final Handler handler = new Handler();
        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                bottomSheetDialog.cancel();
            }
        };
        bottomSheetDialog.setContentView(R.layout.belvedere_bottom_sheet);
        TextView messageTextView = bottomSheetDialog.findViewById(R.id.belvedere_bottom_sheet_message_text);
        if (messageTextView != null) {
            messageTextView.setText(message);
        }
        TextView actionTextView = bottomSheetDialog.findViewById(R.id.belvedere_bottom_sheet_actions_text);
        if (actionTextView != null) {
            actionTextView.setText(actionMessage);
            actionTextView.setOnClickListener(onActionClickListener);
        }
        bottomSheetDialog.setCancelable(true);
        bottomSheetDialog.setOnCancelListener(new OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                handler.removeCallbacks(runnable);
            }
        });
        bottomSheetDialog.setOnDismissListener(new OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                handler.removeCallbacks(runnable);
            }
        });
        bottomSheetDialog.show();
        handler.postDelayed(runnable, length);
    }

    static int getThemeColor(Context context, int attr) {

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
        if (imageView == null || imageView.getDrawable() == null) {
            return;
        }

        Drawable d = DrawableCompat.wrap(imageView.getDrawable());
        DrawableCompat.setTint(d.mutate(), color);

        imageView.invalidate();
    }

    /**
     * Checks whether an app is installed and enabled.
     *
     * @param packageName The package name of an app.
     * @param context     The context.
     * @return true if app is installed and enabled, false otherwise.
     */
    static boolean isAppAvailable(String packageName, Context context) {
        try {
            return context.getPackageManager().getApplicationInfo(packageName, PackageManager.GET_META_DATA).enabled;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    static Transformation roundTransformation(Context context, int radiusResId) {
        final int radius = context.getResources().getDimensionPixelOffset(radiusResId);
        return new RoundedTransformation(radius, 0);
    }

    private static class RoundedTransformation implements Transformation {

        private final int radius, margin;

        RoundedTransformation(final int radius, final int margin) {
            this.radius = radius;
            this.margin = margin;
        }

        @Override
        public Bitmap transform(final Bitmap source) {
            final Paint paint = new Paint();
            paint.setAntiAlias(true);
            paint.setShader(new BitmapShader(source, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP));

            Bitmap output = Bitmap.createBitmap(source.getWidth(), source.getHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(output);
            canvas.drawRoundRect(new RectF(margin, margin, source.getWidth() - margin, source.getHeight() - margin),
                    radius, radius, paint);

            if (source != output) {
                source.recycle();
            }

            return output;
        }

        @Override
        public String key() {
            final String keyFormat = "rounded-%s-%s";
            return String.format(Locale.US, keyFormat, radius, margin);
        }
    }
}
