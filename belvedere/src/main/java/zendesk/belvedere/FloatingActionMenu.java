package zendesk.belvedere;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.os.Build;
import android.support.annotation.AttrRes;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.annotation.StyleRes;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
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


public class FloatingActionMenu extends LinearLayout implements View.OnClickListener {

    private static final float ANIMATION_ROTATION_INITIAL_ANGLE = 0f;

    private View fab;
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
        inflate(context, R.layout.floating_action_menu, this);

        if (!isInEditMode()) {
            setOrientation(LinearLayout.VERTICAL);
            setOnClickListener(this);
            fab = findViewById(R.id.floating_action_menu_fab);
            layoutInflater = LayoutInflater.from(context);
            menuItems = new ArrayList<>();

            final Resources resource = getResources();
            animationDuration = resource.getInteger(R.integer.floating_action_menu_animation_duration);
            animationRotationAngle = resource.getInteger(R.integer.floating_action_menu_animation_rotation_angle);
            animationDelaySubsequentItem = getResources().getInteger(R.integer.floating_action_menu_animation_delay_subsequent_item);
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
        }
    }

    private void showMenuItems(boolean isExpanded) {
        long startOffset = 0;

        if (isExpanded) {
            for (Pair<FloatingActionButton, View.OnClickListener> menuItem : menuItems) {
                Animation a = AnimationUtils.loadAnimation(getContext(), R.anim.show_menu_item);
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

                Animation a = AnimationUtils.loadAnimation(getContext(), R.anim.hide_menu_item);
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

    public void addMenuItem(@DrawableRes int iconId, @NonNull View.OnClickListener clickListener) {
        FloatingActionButton fab = (FloatingActionButton) layoutInflater.inflate(R.layout.floating_action_menu_item, this, false);
        fab.setOnClickListener(clickListener);

        fab.setBackgroundResource(iconId);
        fab.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(getContext(), R.color.floating_action_menu_item_background)));

        addView(fab, 0);
        menuItems.add(Pair.create(fab, clickListener));
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
