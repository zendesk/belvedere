package zendesk.belvedere;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetBehavior;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

import zendesk.belvedere.ui.R;

public class ImageStream extends AppCompatActivity
        implements ImageStreamMvp.View, ImageStreamAdapter.Delegate {

    private static final int PERMISSION_REQUEST_CODE = 9842;

    private static final String VIEW_STATE = "view_state";

    private ImageStreamMvp.Presenter presenter;

    private View bottomSheet, dismissArea;
    private RecyclerView imageList;
    private Toolbar toolbar;
    private MenuItem galleryMenuItem;
    private BottomSheetBehavior<View> bottomSheetBehavior;
    private ImageStreamAdapter imageStreamAdapter;

    private ImageStreamMvp.ViewState viewState;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_image_stream);
        bindViews();

        UiUtils.dimStatusBar(this);
        UiUtils.hideToolbar(this);

        viewState = new ImageStreamMvp.ViewState(BottomSheetBehavior.STATE_COLLAPSED);
        if (savedInstanceState != null && savedInstanceState.getParcelable(VIEW_STATE) != null) {
            viewState = savedInstanceState.getParcelable(VIEW_STATE);
        }

        PermissionStorage preferences = new PermissionStorage(this);
        final List<MediaIntent> mediaIntents = BelvedereUi.getMediaIntents(getIntent().getExtras());
        final ImageStreamMvp.Model model = new ImageStreamModel(this, mediaIntents, preferences);

        presenter = new ImageStreamPresenter(model, this);
        presenter.init();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                presenter.permissionGranted(true, permissions[0]);
            } else if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_DENIED) {
                boolean showRationale = ActivityCompat.shouldShowRequestPermissionRationale(this, permissions[0]);
                if (!showRationale) {
                    presenter.dontAskForPermissionAgain(permissions[0]);
                } else {
                    presenter.permissionGranted(false, permissions[0]);
                }
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Belvedere.from(this).getFilesFromActivityOnResult(requestCode, resultCode, data, new Callback<List<MediaResult>>() {
            @Override
            public void success(List<MediaResult> result) {
                finishWithResult(result);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.imagestream_menu, menu);
        galleryMenuItem = menu.findItem(R.id.image_stream_system_gallery);
        presenter.initMenu();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;

        } else if (item.getItemId() == R.id.image_stream_system_gallery) {
            openGallery();
            return true;

        } else {
            return super.onOptionsItemSelected(item);

        }
    }

    @Override
    public void onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState);
        if (bottomSheetBehavior != null) {
            outState.putParcelable(VIEW_STATE, new ImageStreamMvp.ViewState(bottomSheetBehavior.getState()));
        }
    }

    @Override
    public void finish() {
        super.finish();
        if (bottomSheetBehavior.getState() != BottomSheetBehavior.STATE_EXPANDED
                || Build.VERSION.SDK_INT < 23) { //TODO check
            overridePendingTransition(R.anim.no_change, R.anim.slide_out);
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if (bottomSheetBehavior.getState() != BottomSheetBehavior.STATE_EXPANDED
                || Build.VERSION.SDK_INT < 23) { //TODO check
            overridePendingTransition(R.anim.no_change, R.anim.slide_out);
        }
    }

    @Override
    public void initUiComponents() {
        initToolbar();
        initBottomSheet();
    }

    @Override
    public boolean isPermissionGranted(String permission) {
        return PermissionUtil.isPermissionGranted(this, permission);
    }

    @Override
    public void askForPermission(String permission) {
        final String[] permissions = {permission};
        ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE);
    }

    @Override
    public void showImageStream(List<Uri> images, boolean showCamera) {

        int columns = getResources().getBoolean(R.bool.bottom_sheet_portrait) ? 2 : 3;

        final ImageStreamAdapter adapter = new ImageStreamAdapter(this, images, showCamera);
        final StaggeredGridLayoutManager staggeredGridLayoutManager =
                new StaggeredGridLayoutManager(columns, StaggeredGridLayoutManager.VERTICAL);
        // https://code.google.com/p/android/issues/detail?id=230295
        staggeredGridLayoutManager.setItemPrefetchEnabled(false);

        initRecycler(adapter, staggeredGridLayoutManager);
    }

    @Override
    public void showList(MediaIntent cameraIntent, MediaIntent documentIntent) {
        final ImageStreamAdapter adapter = new ImageStreamAdapter(this);
        final LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        initRecycler(adapter, linearLayoutManager);
    }

    @Override
    public void showDocumentMenuItem(boolean visible) {
        if (galleryMenuItem != null) {
            galleryMenuItem.setVisible(visible);
        }
    }

    @Override
    public void openMediaIntent(MediaIntent mediaIntent) {
        mediaIntent.open(this);
    }

    @Override
    public void finishWithoutResult() {
        setResult(RESULT_CANCELED);
        finish();
    }

    @Override
    public void finishIfNothingIsLeft() {
        if (imageStreamAdapter == null) {
            finishWithoutResult();
        }
    }

    @Override
    public void hideCameraOption() {
        if (imageStreamAdapter != null) {
            imageStreamAdapter.hideCameraOption();
        } else {
            finishWithoutResult();
        }
    }

    @Override
    public void imagesSelected(List<Uri> uris) {
        Belvedere.from(this).resolveUris(uris, new Callback<List<MediaResult>>() {
            @Override
            public void success(List<MediaResult> result) {
                finishWithResult(result);
            }
        });
    }

    @Override
    public void openCamera() {
        presenter.openCamera();
    }

    @Override
    public void openGallery() {
        presenter.openGallery();
    }

    private void initRecycler(ImageStreamAdapter adapter, RecyclerView.LayoutManager layoutManager) {
        this.imageStreamAdapter = adapter;
        imageList.setAdapter(adapter);
        imageList.setItemAnimator(null);
        imageList.setLayoutManager(layoutManager);
        imageList.setHasFixedSize(true);
    }

    private void finishWithResult(List<MediaResult> belvedereResults) {
        final Intent intent = ImageStream.this.getIntent();
        intent.putParcelableArrayListExtra(MediaSource.INTERNAL_RESULT_KEY, new ArrayList<>(belvedereResults));
        setResult(RESULT_OK, intent);
        finish();
    }

    private void initToolbar() {
        toolbar.setNavigationIcon(R.drawable.belvedere_ic_close);
        setSupportActionBar(toolbar);
        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle("Photo library");
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    private void bindViews() {
        this.bottomSheet = findViewById(R.id.bottom_sheet);
        this.dismissArea = findViewById(R.id.dismiss_area);
        this.imageList = (RecyclerView) findViewById(R.id.image_list);
        this.toolbar = (Toolbar) findViewById(R.id.image_stream_toolbar);
    }

    private void initBottomSheet() {
        bottomSheet.setVisibility(View.VISIBLE);

        ViewCompat.setElevation(imageList, getResources().getDimensionPixelSize(R.dimen.bottom_sheet_elevation));

        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        bottomSheetBehavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                switch (newState) {
                    case BottomSheetBehavior.STATE_HIDDEN:
                        finish();
                        break;
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                float offset = 0.6f;
                if (slideOffset >= offset) {
                    ViewCompat.setAlpha(toolbar, 1f - (1f - slideOffset) / (1f - offset));
                    UiUtils.showToolbar(ImageStream.this);
                } else {
                    UiUtils.hideToolbar(ImageStream.this);
                }
            }
        });

        if (viewState.getBottomSheetState() == BottomSheetBehavior.STATE_EXPANDED) {
            UiUtils.showToolbar(ImageStream.this);
        } else {
            UiUtils.hideToolbar(ImageStream.this);
        }

        bottomSheetBehavior.setState(viewState.getBottomSheetState());
        dismissArea.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        bottomSheet.setClickable(true);
    }
}