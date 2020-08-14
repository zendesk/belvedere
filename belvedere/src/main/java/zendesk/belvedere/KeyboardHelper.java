package zendesk.belvedere;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Rect;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * Here be dragons
 */
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

    static void showKeyboard(final EditText editText) {
        editText.post(new Runnable() {
            @Override
            public void run() {
                if(editText.requestFocus()) {
                    InputMethodManager imm = (InputMethodManager) editText.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    if(imm != null) {
                        imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT);
                    }
                }
            }
        });
    }

    static void hideKeyboard(final Activity activity) {
        final View view = activity.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
    }

    private final int statusBarHeight;

    private int viewInset = -1;
    private int keyboardHeight = -1;
    private boolean isKeyboardVisible = false;

    private List<WeakReference<Listener>> keyboardListener = new ArrayList<>();
    private SizeListener keyboardSizeListener;

    private EditText inputTrap;

    private KeyboardHelper(@NonNull Activity activity) {
        super(activity);
        this.statusBarHeight = getStatusBarHeight();
        int sizeForDummyView = activity.getResources()
                .getDimensionPixelSize(zendesk.belvedere.ui.R.dimen.belvedere_dummy_edit_text_size);
        setLayoutParams(new ViewGroup.LayoutParams(sizeForDummyView, sizeForDummyView));

        inputTrap = new EditText(activity);
        inputTrap.setFocusable(true);
        inputTrap.setFocusableInTouchMode(true);
        inputTrap.setVisibility(View.VISIBLE);
        inputTrap.setImeOptions(EditorInfo.IME_FLAG_NO_EXTRACT_UI);
        inputTrap.setInputType(EditorInfo.TYPE_TEXT_FLAG_CAP_SENTENCES);

        addView(inputTrap);

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
                // well .... at least we tried
            }
        }

        return 0;
    }

    public EditText getInputTrap() {
        return inputTrap;
    }

    public int getKeyboardHeight() {
        return keyboardHeight;
    }

    public boolean isKeyboardVisible() {
        return isKeyboardVisible;
    }

    public void addListener(Listener keyboardListener) {
        this.keyboardListener.add(new WeakReference<>(keyboardListener));
    }

    void setKeyboardHeightListener(SizeListener sizeListener) {
        this.keyboardSizeListener = sizeListener;
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

                if(keyboardSizeListener != null) {
                    keyboardSizeListener.onSizeChanged(keyboardHeight);
                }
            }

            if(keyboardListener != null && keyboardHeight > 0) {
                notifyKeyboardVisible();
            } else {
                notifyKeyboardDismissed();
            }
        }
    }

    private void notifyKeyboardVisible() {
        for(WeakReference<Listener> listeners : keyboardListener) {
            if(listeners.get() != null) {
                listeners.get().onKeyboardVisible();
            }
        }
    }

    private void notifyKeyboardDismissed() {
        for(WeakReference<Listener> listeners : keyboardListener) {
            if(listeners.get() != null) {
                listeners.get().onKeyboardDismissed();
            }
        }
    }

    public interface Listener {
        void onKeyboardVisible();
        void onKeyboardDismissed();
    }

    interface SizeListener {
        void onSizeChanged(int keyboardHeight);
    }

}
