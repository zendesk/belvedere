package zendesk.belvedere;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Build;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;

import androidx.annotation.AttrRes;
import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.annotation.StringRes;
import androidx.annotation.StyleRes;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.util.Pair;
import androidx.core.view.ViewCompat;
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

import zendesk.belvedere.ui.R;

/**
 * For internal use only.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class FloatingActionMenu extends LinearLayout implements View.OnClickListener {

    private static final float ANIMATION_ROTATION_INITIAL_ANGLE = 0f;
    private static final int ANIMATION_DURATION = 150;

    private FloatingActionButton fab;
    private LayoutInflater layoutInflater;
    private List<Pair<FloatingActionButton, View.OnClickListener>> menuItems;
    private OnClickListener onSendClickListener;
    private boolean isExpanded;
    private boolean isShowingSend;
    private int animationDuration;
    private int animationRotationAngle;
    private int animationDelaySubsequentItem;


    public FloatingActionMenu(@NonNull Context context) {
        super(context);
        initView(context);
    }

    public FloatingActionMenu(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initView(context);
    }

    public FloatingActionMenu(@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(context);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public FloatingActionMenu(@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr, @StyleRes int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initView(context);
    }

    private void initView(@NonNull Context context) {
        inflate(context, R.layout.belvedere_floating_action_menu, this);

        if (!isInEditMode()) {
            setOrientation(LinearLayout.VERTICAL);
            setOnClickListener(this);
            fab = findViewById(R.id.floating_action_menu_fab);
            fab.setOnClickListener(this);
            layoutInflater = LayoutInflater.from(context);
            menuItems = new ArrayList<>();

            final Resources resource = getResources();
            animationDuration = resource.getInteger(R.integer.belvedere_fam_animation_duration);
            animationRotationAngle = resource.getInteger(R.integer.belvedere_fam_animation_rotation_angle);
            animationDelaySubsequentItem = getResources().getInteger(R.integer.belvedere_fam_animation_delay_subsequent_item);
        }
    }

    @Override
    public void onClick(View v) {
        if (isShowingSend && onSendClickListener != null) {
            onSendClickListener.onClick(this);
            return;
        }

        if (menuItems.size() == 1) {
            final Pair<FloatingActionButton, View.OnClickListener> menuItem = menuItems.get(0);
            menuItem.second.onClick(menuItem.first);
        } else {
            toggleMenu();
        }
    }

    public void showSendButton() {
        isShowingSend = true;
        if (isExpanded) {
            hideMenu();
        }
        crossFadeFabIcons(R.drawable.belvedere_fam_icon_add, R.drawable.belvedere_fam_icon_send);

    }

    public void hideSendButton() {
        if(isShowingSend) {
            crossFadeFabIcons(R.drawable.belvedere_fam_icon_send, R.drawable.belvedere_fam_icon_add);
        }
        isShowingSend = false;
    }

    public void setOnSendClickListener(View.OnClickListener onSendClickListener) {
        this.onSendClickListener = onSendClickListener;
    }

    private void toggleMenu() {
        isExpanded = !isExpanded;
        if (isExpanded) {
            showMenu();
        } else {
            hideMenu();
        }
    }

    private void showMenu() {
        rotate(true);
        showMenuItems(true);
        fab.setContentDescription(getResources().getString(R.string.belvedere_fam_desc_expand_fam));
    }

    private void hideMenu() {
        rotate(false);
        showMenuItems(false);
        fab.setContentDescription(getResources().getString(R.string.belvedere_fam_desc_collapse_fam));
    }

    private void showMenuItems(boolean isExpanded) {
        long startOffset = 0;

        if (isExpanded) {
            for (Pair<FloatingActionButton, View.OnClickListener> menuItem : menuItems) {
                Animation a = AnimationUtils.loadAnimation(getContext(), R.anim.belvedere_show_menu_item);
                a.setRepeatMode(Animation.REVERSE);
                a.setStartOffset(startOffset);
                changeVisibility(menuItem.first, VISIBLE);
                menuItem.first.startAnimation(a);

                startOffset += animationDelaySubsequentItem;
            }
        } else {
            Animation lastAnimation = null;

            for (int i = menuItems.size() - 1; i >= 0; i--) {
                final Pair<FloatingActionButton, View.OnClickListener> menuItem = menuItems.get(i);

                Animation a = AnimationUtils.loadAnimation(getContext(), R.anim.belvedere_hide_menu_item);
                a.setRepeatMode(Animation.REVERSE);
                a.setStartOffset(startOffset);
                a.setAnimationListener(new AnimationListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animation animation) {
                        changeVisibility(menuItem.first, INVISIBLE);
                    }
                });
                menuItem.first.startAnimation(a);

                startOffset += animationDelaySubsequentItem;

                lastAnimation = a;
            }

            if (lastAnimation != null) {
                lastAnimation.setAnimationListener(setGone);
            }
        }
    }

    private void rotate(boolean isExpanded) {
        float angle = isExpanded ? animationRotationAngle : ANIMATION_ROTATION_INITIAL_ANGLE;
        ViewCompat.animate(fab).rotation(angle).setDuration(animationDuration).start();
    }

    public void addMenuItem(@DrawableRes int iconRes, @IdRes int id, @StringRes int contentDescription, @NonNull View.OnClickListener clickListener) {
        FloatingActionButton menuItem = (FloatingActionButton) layoutInflater.inflate(R.layout.belvedere_floating_action_menu_item, this, false);
        menuItem.setOnClickListener(clickListener);
        menuItem.setImageDrawable(getTintedDrawable(iconRes, R.color.belvedere_floating_action_menu_item_icon_color));
        menuItem.setId(id);
        menuItem.setContentDescription(getResources().getString(contentDescription));

        menuItems.add(Pair.create(menuItem, clickListener));

        if (menuItems.size() == 1) {
            fab.setImageDrawable(getTintedDrawable(iconRes, R.color.belvedere_floating_action_menu_icon_color));
            fab.setContentDescription(getResources().getString(contentDescription));

        } else if (menuItems.size() == 2) {
            addView(menuItems.get(0).first, 0);
            addView(menuItem, 0);

            fab.setImageDrawable(getTintedDrawable(R.drawable.belvedere_fam_icon_add, R.color.belvedere_floating_action_menu_icon_color));
            fab.setContentDescription(getResources().getString(R.string.belvedere_fam_desc_expand_fam));
        } else {
            addView(menuItem, 0);
        }
        setVisibility(VISIBLE);
    }

    private final AnimationListenerAdapter setGone = new AnimationListenerAdapter() {
        @Override
        public void onAnimationEnd(Animation animation) {
            for (Pair<FloatingActionButton, OnClickListener> menuItem : menuItems) {
                changeVisibility(menuItem.first, GONE);
            }
        }
    };

    private Drawable getTintedDrawable(@DrawableRes int drawableRes, @ColorRes int colorRes) {
        final Context context = getContext();
        final Drawable originalDrawable = ContextCompat.getDrawable(context, drawableRes);
        final Drawable wrappedDrawable = DrawableCompat.wrap(originalDrawable);
        DrawableCompat.setTint(wrappedDrawable, ContextCompat.getColor(context, colorRes));
        return wrappedDrawable;
    }

    private void crossFadeFabIcons(@DrawableRes final int fromDrawable, @DrawableRes final int toDrawable) {
        final Drawable fromVector = ResourcesCompat.getDrawable(getResources(),
                fromDrawable, getContext().getTheme());
        final Drawable toVector = ResourcesCompat.getDrawable(getResources(),
                toDrawable, getContext().getTheme());
        fromVector.setAlpha(255);
        toVector.setAlpha(0);
        LayerDrawable layerDrawable = new LayerDrawable(new Drawable[]{fromVector, toVector});
        fab.setImageDrawable(layerDrawable);
        ValueAnimator fadeAnimator = ValueAnimator.ofInt(0, 255);
        fadeAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                int alpha = Integer.parseInt(animation.getAnimatedValue().toString());
                toVector.setAlpha(alpha); //fade add icon in
                fromVector.setAlpha(255 - alpha); //fade send icon out
            }
        });
        fadeAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                fab.setImageResource(toDrawable);
            }
        });
        fadeAnimator.setDuration(ANIMATION_DURATION);
        fadeAnimator.start();
    }

    private void changeVisibility(@Nullable View view, int visibility) {
        if (view != null) {
            view.setVisibility(visibility);
        }
    }

    private abstract static class AnimationListenerAdapter implements Animation.AnimationListener {

        @Override
        public void onAnimationStart(Animation animation) {
            // Intentionally empty
        }

        @Override
        public void onAnimationEnd(Animation animation) {
            // Intentionally empty
        }

        @Override
        public void onAnimationRepeat(Animation animation) {
            // Intentionally empty
        }
    }
}
