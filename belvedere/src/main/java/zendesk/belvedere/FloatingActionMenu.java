package zendesk.belvedere;

import android.content.Context;
import android.content.res.Resources;
import android.os.Build;
import android.support.annotation.AttrRes;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.annotation.StyleRes;
import android.support.v4.util.Pair;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import java.util.LinkedList;
import java.util.List;

import zendesk.belvedere.ui.R;


public class FloatingActionMenu extends LinearLayout implements View.OnClickListener {

    private static final float ANIMATION_ROTATION_INITIAL_ANGLE = 0f;

    private View fab;
    private LayoutInflater layoutInflater;
    private List<Pair<ImageView, View.OnClickListener>> menuItems;
    private boolean isExpanded;
    private int animationDuration;
    private int animationRotationAngle;


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
            menuItems = new LinkedList<>();

            final Resources resource = getResources();
            animationDuration = resource.getInteger(R.integer.floating_action_menu_animation_duration);
            animationRotationAngle = resource.getInteger(R.integer.floating_action_menu_animation_rotation_angle);
        }
    }

    @Override
    public void onClick(View v) {
        if (menuItems.size() == 1) {
            final Pair<ImageView, View.OnClickListener> menuItem = menuItems.get(0);
            menuItem.second.onClick(menuItem.first);
        } else {
            isExpanded = !isExpanded;
            showMenuItems(isExpanded);
            rotate(isExpanded);
        }
    }

    private void showMenuItems(boolean isExpanded) {
        for (Pair<ImageView, OnClickListener> menuItem : menuItems) {
            menuItem.first.setVisibility(isExpanded ? VISIBLE : GONE);
        }
    }

    private void rotate(boolean isExpanded) {
        final float angle = isExpanded ? animationRotationAngle : ANIMATION_ROTATION_INITIAL_ANGLE;
        ViewCompat.animate(fab).rotation(angle).setDuration(animationDuration).start();
    }

    public void addMenuItem(@DrawableRes int iconId, @NonNull View.OnClickListener clickListener) {
        ImageView imageView = (ImageView) layoutInflater.inflate(R.layout.floating_action_menu_item, this, false);
        imageView.setOnClickListener(clickListener);
        imageView.setImageResource(iconId);

        addView(imageView, 0);
        menuItems.add(Pair.create(imageView, clickListener));
        setVisibility(VISIBLE);
    }
}
