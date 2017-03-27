package zendesk.belvedere;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Rect;
import android.os.Build;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.widget.FrameLayout;

import java.lang.reflect.Field;

@SuppressLint("ViewConstructor")
public class KeyboardHelper extends FrameLayout {

    public static KeyboardHelper inject(Activity activity) {
        final ViewGroup decorView = (ViewGroup) activity.getWindow().getDecorView();

        for(int i = 0, c = decorView.getChildCount(); i < c; i++) {
            if(decorView.getChildAt(i) instanceof KeyboardHelper) {
                return (KeyboardHelper) decorView.getChildAt(i);
            }
        }

        final KeyboardHelper keyboardHelper = new KeyboardHelper(activity);
        decorView.addView(keyboardHelper);
        return keyboardHelper;
    }

    private final int statusBarHeight;

    private int viewInset = -1;
    private int keyboardHeight = -1;
    private boolean isKeyboardVisible = false;
    private Listener keyboardListener;

    private KeyboardHelper(@NonNull Activity activity) {
        super(activity);
        this.statusBarHeight = getStatusBarHeight();
        setLayoutParams(new ViewGroup.LayoutParams(0, 0));

        final View rootView = activity.getWindow().getDecorView().findViewById(Window.ID_ANDROID_CONTENT);
        rootView.getViewTreeObserver().addOnGlobalLayoutListener(new KeyboardTreeObserver(activity));
    }

    private int getKeyboardHeight(Activity activity) {
        final Rect r = new Rect();
        final View view = activity.getWindow().getDecorView();
        view.getWindowVisibleDisplayFrame(r);
        return getViewPortHeight() - (r.bottom - r.top);
    }

    private int getViewPortHeight() {
        return getRootView().getHeight() - statusBarHeight - getCachedInset();
    }

    private int getStatusBarHeight() {
        final int statusBarRes = getResources().getIdentifier("status_bar_height", "dimen", "android");
        return statusBarRes > 0 ? getResources().getDimensionPixelSize(statusBarRes) : 0;
    }

    private int getCachedInset() {
        if(viewInset == -1) {
            viewInset = getViewInset();
        }

        return viewInset;
    }

    private int getViewInset() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                Field attachInfoField = View.class.getDeclaredField("mAttachInfo");
                attachInfoField.setAccessible(true);
                Object attachInfo = attachInfoField.get(this);
                if (attachInfo != null) {
                    Field stableInsetsField = attachInfo.getClass().getDeclaredField("mStableInsets");
                    stableInsetsField.setAccessible(true);
                    Rect insets = (Rect)stableInsetsField.get(attachInfo);

                    return insets.bottom;
                }
            } catch (Exception e) {
                // well ....
            }
        }

        return 0;
    }

    public int getKeyboardHeight() {
        return keyboardHeight;
    }

    public boolean isKeyboardVisible() {
        return isKeyboardVisible;
    }

    public void setListener(Listener keyboardListener) {
        this.keyboardListener = keyboardListener;
    }

    private class KeyboardTreeObserver implements ViewTreeObserver.OnGlobalLayoutListener {

        private final Activity activity;

        private KeyboardTreeObserver(Activity activity) {
            this.activity = activity;
        }

        @Override
        public void onGlobalLayout() {
            final int keyboardHeight = getKeyboardHeight(activity);
            KeyboardHelper.this.isKeyboardVisible = keyboardHeight > 0;

            if(keyboardHeight > 0 && KeyboardHelper.this.keyboardHeight != keyboardHeight) {
                KeyboardHelper.this.keyboardHeight = keyboardHeight;
            }

            if(keyboardListener != null && keyboardHeight > 0) {
                keyboardListener.onKeyboardVisible();
            }
        }
    }

    public interface Listener {
        void onKeyboardVisible();
    }

}
