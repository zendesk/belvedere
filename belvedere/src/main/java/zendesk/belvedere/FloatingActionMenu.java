package zendesk.belvedere;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
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

    private static final String GOOGLE_PHOTOS_PACKAGE_NAME = "com.google.android.apps.photos";

    private boolean googlePhotosAvailable;
    private Activity activity;

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

    private void initView(@NonNull Context context) {
        inflate(context, R.layout.floating_action_menu, this);

        if (!isInEditMode()) {
            btnAddFile = findViewById(R.id.add_file_btn);
            btnAddPhoto = findViewById(R.id.add_photo_btn);
        }
    }

    void init(Activity activity) {
        this.activity = activity;
        googlePhotosAvailable = Utils.isAppAvailable(GOOGLE_PHOTOS_PACKAGE_NAME, activity);

        findViewById(R.id.fam).setOnClickListener(this);
        btnAddFile.setOnClickListener(this);
        btnAddPhoto.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (v.getVisibility() == VISIBLE) {
            if (v.getId() == R.id.add_file_btn) {
                openFilePicker();
            } else if (v.getId() == R.id.add_photo_btn) {
                openGooglePhotos();
            }
        }

        if (v.getId() == R.id.fam && !googlePhotosAvailable) {
            openFilePicker();
        }

        toggle();
    }

    private void toggle() {
        boolean isVisible = btnAddFile.getVisibility() == VISIBLE;

        btnAddFile.setVisibility(isVisible ? GONE : VISIBLE);
        if (googlePhotosAvailable) {
            btnAddPhoto.setVisibility(isVisible ? GONE : VISIBLE);
        }
    }

    private void openFilePicker() {
        Belvedere.from(activity).document().build().open(activity);
    }

    private void openGooglePhotos() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        activity.startActivityForResult(intent, 0);
    }
}
