package zendesk.belvedere;

import android.animation.ValueAnimator;
import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;

import zendesk.belvedere.ui.R;

/**
 * For internal use only.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class SelectableView extends FrameLayout implements View.OnClickListener {

    private final static long ANIMATION_DURATION = 75L;
    private final static float SELECTED_SCALE = .9F;
    private final static float SELECTED_ALPHA = .8F;

    private SelectionListener selectionListener;
    private View child;
    private View checkbox;

    private String descSelected, descUnselected;

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

        final int colorPrimary = Utils.getThemeColor(getContext(), R.attr.colorPrimary);
        checkbox = getCheckBox(colorPrimary);
        addView(checkbox);
    }

    public void setContentDescriptionStrings(String selected, String unselected) {
        descSelected = selected;
        descUnselected = unselected;
        setContentDesc(isSelected());
    }

    @Override
    public void onClick(final View view) {
        final boolean selected = !isSelected();

        final boolean changeSelection;
        if (selectionListener != null) {
            changeSelection = selectionListener.onSelectionChanged(selected);
        } else {
            changeSelection = true;
        }

        if (changeSelection) {
            setSelected(selected);

            final ValueAnimator scaleAnimator;
            final ValueAnimator alphaAnimator;
            if (selected) {
                scaleAnimator = ValueAnimator.ofFloat(1F, SELECTED_SCALE);
                alphaAnimator = ValueAnimator.ofFloat(1F, SELECTED_ALPHA);
            } else {
                scaleAnimator = ValueAnimator.ofFloat(SELECTED_SCALE, 1F);
                alphaAnimator = ValueAnimator.ofFloat(SELECTED_ALPHA, 1F);
            }

            scaleAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator valueAnimator) {
                    scale((float) valueAnimator.getAnimatedValue());
                }
            });

            alphaAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator valueAnimator) {
                    alpha((float) valueAnimator.getAnimatedValue());
                }
            });

            alphaAnimator.setDuration(ANIMATION_DURATION);
            scaleAnimator.setDuration(ANIMATION_DURATION);

            scaleAnimator.start();
            alphaAnimator.start();
        }
    }

    @Override
    public void setSelected(boolean selected) {
        super.setSelected(selected);

        if(selected) {
            scale(SELECTED_SCALE);
            alpha(SELECTED_ALPHA);
            checkbox(true);
            setContentDesc(true);

        } else {
            scale(1.0F);
            alpha(1.0F);
            checkbox(false);
            setContentDesc(false);
        }
    }

    public void setSelectionListener(SelectionListener selectionListener) {
        this.selectionListener = selectionListener;
    }

    private ImageView getCheckBox(int colorPrimary) {
        final FrameLayout.LayoutParams params = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.CENTER;

        final ImageView imageView = new ImageView(getContext());
        imageView.setId(R.id.belvedere_selectable_view_checkbox);
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

        if(getChildCount() != 2) {
            throw new RuntimeException("More then one child added to SelectableView");
        }

        for(int i = 0; i < getChildCount(); i++) {
            final View c = getChildAt(i);
            if(c.getId() != R.id.belvedere_selectable_view_checkbox) {
                child = c;
                break;
            }
        }

        return child;
    }

    private void setContentDesc(boolean selected) {
        if (selected) {
            setContentDescription(descSelected);
        } else {
            setContentDescription(descUnselected);
        }
    }

    interface SelectionListener {
        boolean onSelectionChanged(boolean selected);
    }
}
