package zendesk.belvedere;

import android.content.Context;
import android.os.Build;
import android.support.annotation.AttrRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.annotation.StyleRes;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;

import zendesk.belvedere.ui.R;


public class FloatingActionMenu extends LinearLayout implements View.OnClickListener {

    interface MenuItemClickListener {
        void onAddFileClicked();
        void onAddPhotoClicked();
    }

    private MenuItemClickListener menuItemClickListener;
    private View btnAddFile;
    private View btnAddPhoto;

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

    @SuppressWarnings("unused")
    public void setMenuItemClickListener(@Nullable MenuItemClickListener menuItemClickListener) {
        this.menuItemClickListener = menuItemClickListener;
    }

    private void initView(@NonNull Context context) {
        inflate(context, R.layout.floating_action_menu, this);

        if (!isInEditMode()) {
            findViewById(R.id.fam).setOnClickListener(this);
            btnAddFile = findViewById(R.id.add_file_btn);
            btnAddPhoto = findViewById(R.id.add_photo_btn);

            btnAddFile.setOnClickListener(this);
            btnAddPhoto.setOnClickListener(this);
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getVisibility() == VISIBLE && menuItemClickListener != null) {
            if (v.getId() == R.id.add_file_btn) {
                menuItemClickListener.onAddFileClicked();
            } else if (v.getId() == R.id.add_photo_btn) {
                menuItemClickListener.onAddPhotoClicked();
            }
        }

        toggle();
    }

    private void toggle() {
        boolean isVisible = btnAddFile.getVisibility() == VISIBLE;

        btnAddFile.setVisibility(isVisible ? GONE : VISIBLE);
        btnAddPhoto.setVisibility(isVisible ? GONE : VISIBLE);
    }
}
