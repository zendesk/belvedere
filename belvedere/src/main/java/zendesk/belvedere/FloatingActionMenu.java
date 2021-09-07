package zendesk.belvedere;

import android.content.Context;
import android.graphics.drawable.Drawable;
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
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.util.Pair;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

import zendesk.belvedere.ui.R;

/**
 * For internal use only.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class FloatingActionMenu extends LinearLayout implements View.OnClickListener {

    private FloatingActionButton fab;
    private LayoutInflater layoutInflater;
    private final List<Pair<FloatingActionButton, View.OnClickListener>> menuItems = new ArrayList<>();
    private OnClickListener onSendClickListener;
    private boolean isExpanded;
    private boolean isShowingSend = true;
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
            animationDelaySubsequentItem = getResources().getInteger(R.integer.belvedere_fam_animation_delay_subsequent_item);
            showSendButton();
        }
    }

    @Override
    public void onClick(View v) {
        if (isShowingSend && onSendClickListener != null) {
            onSendClickListener.onClick(this);
            return;
        }

        if (menuItems.isEmpty()) {
            //no menu items, do nothing
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
        fab.setImageResource(R.drawable.belvedere_fam_icon_send);

    }

    public void hideSendButton() {
        if(menuItems.isEmpty()) {
            //no menu, keep showing send button
            return;
        }
        if(isShowingSend) {
            fab.setImageResource(R.drawable.belvedere_fam_icon_add_file);
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
        if (menuItems.isEmpty()) {
            showSendButton();
            return;
        }

        long startOffset = 0;

        if (isExpanded) {
            for (Pair<FloatingActionButton, View.OnClickListener> menuItem : menuItems) {
                Animation a = AnimationUtils.loadAnimation(getContext(), R.anim.belvedere_show_menu_item);
                a.setRepeatMode(Animation.REVERSE);
                a.setStartOffset(startOffset);
                if (menuItem.first != null) {
                    changeVisibility(menuItem.first, VISIBLE);
                    menuItem.first.startAnimation(a);
                }

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
                if (menuItem.first != null) {
                    menuItem.first.startAnimation(a);
                }

                startOffset += animationDelaySubsequentItem;

                lastAnimation = a;
            }

            if (lastAnimation != null) {
                lastAnimation.setAnimationListener(setGone);
            }
        }
    }

    private void rotate(boolean isExpanded) {
        if (isExpanded) {
            fab.setImageResource(R.drawable.belvedere_fam_icon_close);
        } else {
            fab.setImageResource(R.drawable.belvedere_fam_icon_add_file);
        }

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

            fab.setImageDrawable(getTintedDrawable(R.drawable.belvedere_fam_icon_add_file, R.color.belvedere_floating_action_menu_icon_color));
            fab.setContentDescription(getResources().getString(R.string.belvedere_fam_desc_expand_fam));
        } else {
            addView(menuItem, 0);
        }

        if(!menuItems.isEmpty()) {
            hideSendButton();
        }
    }

    private Drawable getTintedDrawable(@DrawableRes int drawableRes, @ColorRes int colorRes) {
        final Context context = getContext();
        final Drawable originalDrawable = ContextCompat.getDrawable(context, drawableRes);
        final Drawable wrappedDrawable = DrawableCompat.wrap(originalDrawable);
        DrawableCompat.setTint(wrappedDrawable, ContextCompat.getColor(context, colorRes));
        return wrappedDrawable;
    }

    private void changeVisibility(@Nullable View view, int visibility) {
        if (view != null) {
            view.setVisibility(visibility);
        }
    }

    private final AnimationListenerAdapter setGone = new AnimationListenerAdapter() {
        @Override
        public void onAnimationEnd(Animation animation) {
            for (Pair<FloatingActionButton, OnClickListener> menuItem : menuItems) {
                changeVisibility(menuItem.first, GONE);
            }
        }
    };

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
