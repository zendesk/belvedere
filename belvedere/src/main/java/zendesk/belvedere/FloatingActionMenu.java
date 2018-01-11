package zendesk.belvedere;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.AttrRes;
import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.annotation.RestrictTo;
import android.support.annotation.StringRes;
import android.support.annotation.StyleRes;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.util.Pair;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.List;

import zendesk.belvedere.ui.R;


@RestrictTo(RestrictTo.Scope.LIBRARY)
public class FloatingActionMenu extends LinearLayout implements View.OnClickListener {

    private static final float ANIMATION_ROTATION_INITIAL_ANGLE = 0f;

    private FloatingActionButton fab;
    private LayoutInflater layoutInflater;
    private List<Pair<FloatingActionButton, View.OnClickListener>> menuItems;
    private boolean isExpanded;
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
        if (menuItems.size() == 1) {
            final Pair<FloatingActionButton, View.OnClickListener> menuItem = menuItems.get(0);
            menuItem.second.onClick(menuItem.first);
        } else {
            isExpanded = !isExpanded;
            showMenuItems(isExpanded);
            rotate(isExpanded);

            if (isExpanded) {
                fab.setContentDescription(getResources().getString(R.string.belvedere_fam_desc_collapse_fam));
            } else {
                fab.setContentDescription(getResources().getString(R.string.belvedere_fam_desc_expand_fam));
            }
        }
    }

    private void showMenuItems(boolean isExpanded) {
        long startOffset = 0;

        if (isExpanded) {
            for (Pair<FloatingActionButton, View.OnClickListener> menuItem : menuItems) {
                Animation a = AnimationUtils.loadAnimation(getContext(), R.anim.belvedere_show_menu_item);
                a.setRepeatMode(Animation.REVERSE);
                a.setStartOffset(startOffset);
                menuItem.first.setVisibility(VISIBLE);
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
                        menuItem.first.setVisibility(INVISIBLE);
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

    private AnimationListenerAdapter setGone = new AnimationListenerAdapter() {
        @Override
        public void onAnimationEnd(Animation animation) {
            for (Pair<FloatingActionButton, OnClickListener> menuItem : menuItems) {
                menuItem.first.setVisibility(GONE);
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

    private abstract class AnimationListenerAdapter implements Animation.AnimationListener {

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
