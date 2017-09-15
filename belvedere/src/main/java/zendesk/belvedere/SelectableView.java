package zendesk.belvedere;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import zendesk.belvedere.ui.R;

public class SelectableView extends FrameLayout implements View.OnClickListener {

    private final static long ANIMATION_DURATION = 75L;
    private final static float SELECTED_SCALE = .9F;
    private final static float SELECTED_ALPHA = .8F;

    private SelectionListener selectionListener;
    private View child;
    private View checkbox;

    public SelectableView(@NonNull Context context) {
        super(context);
        init();
    }

    public SelectableView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SelectableView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setClickable(true);
        setFocusable(true);
        setOnClickListener(this);

        int colorPrimary = Utils.getThemeColor(getContext(), R.attr.colorPrimary);
        checkbox = getCheckBox(colorPrimary);
        addView(checkbox);
    }

    @Override
    public void onClick(final View view) {
        final boolean selected = !isSelected();

        final ValueAnimator scaleAnimator;
        final ValueAnimator alphaAnimator;
        if(selected) {
            scaleAnimator = ValueAnimator.ofFloat(1F, SELECTED_SCALE);
            alphaAnimator = ValueAnimator.ofFloat(1F, SELECTED_ALPHA);
        } else {
            scaleAnimator = ValueAnimator.ofFloat(SELECTED_SCALE, 1F);
            alphaAnimator = ValueAnimator.ofFloat(SELECTED_ALPHA, 1F);
        }

        scaleAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                scale((Float) valueAnimator.getAnimatedValue());
            }
        });

        alphaAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                alpha((Float) valueAnimator.getAnimatedValue());
            }
        });

        alphaAnimator.setDuration(ANIMATION_DURATION);
        scaleAnimator.setDuration(ANIMATION_DURATION);

        scaleAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                setSelected(selected);

                if(selectionListener != null) {
                    selectionListener.onSelectionChanged(selected);
                }
            }
        });

        scaleAnimator.start();
        alphaAnimator.start();
    }

    @Override
    public void setSelected(boolean selected) {
        super.setSelected(selected);
        if(selected) {
            scale(SELECTED_SCALE);
            alpha(SELECTED_ALPHA);
            checkbox(true);
        } else {
            scale(1.0F);
            alpha(1.0F);
            checkbox(false);
        }
    }

    public void setSelectionListener(SelectionListener selectionListener) {
        this.selectionListener = selectionListener;
    }

    private ImageView getCheckBox(int colorPrimary) {
        final FrameLayout.LayoutParams params = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.CENTER;

        final ImageView imageView = new ImageView(getContext());
        imageView.setTag(getId() + "_checkbox");
        imageView.setImageDrawable(ContextCompat.getDrawable(getContext(), R.drawable.belvedere_ic_check_circle));
        ViewCompat.setBackground(imageView, ContextCompat.getDrawable(getContext(), R.drawable.belvedere_ic_check_bg));
        imageView.setLayoutParams(params);
        imageView.setVisibility(View.GONE);
        Utils.internalSetTint(imageView, colorPrimary);

        return imageView;
    }

    private void checkbox(boolean visible) {
        if(visible) {
            checkbox.setVisibility(View.VISIBLE);
            checkbox.bringToFront();
            ViewCompat.setElevation(checkbox, ViewCompat.getElevation(child) + 1);
        } else {
            checkbox.setVisibility(View.GONE);
        }
    }

    private void scale(float value) {
        getChild().setScaleX(value);
        getChild().setScaleY(value);
    }

    private void alpha(float value) {
        getChild().setAlpha(value);
    }

    private View getChild() {

        if(child != null) {
            return child;
        }

        if(getChildCount() > 2) {
            throw new RuntimeException("Only a single view is supported");
        }

        if(getChildAt(0).getTag().equals(getId() + "_checkbox")) {
            child = getChildAt(1);
        } else {
            child = getChildAt(0);
        }

        return child;
    }

    interface SelectionListener {
        void onSelectionChanged(boolean selected);
    }
}
