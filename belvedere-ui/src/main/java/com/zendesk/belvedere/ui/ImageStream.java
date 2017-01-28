package com.zendesk.belvedere.ui;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetBehavior;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.zendesk.belvedere.Belvedere;
import com.zendesk.belvedere.Callback;
import com.zendesk.belvedere.BelvedereResult;

import java.util.ArrayList;
import java.util.List;

public class ImageStream extends AppCompatActivity
        implements ImageStreamMvp.View, ImageStreamAdapter.Delegate {

    private static final int PERMISSION_REQUEST_CODE = 132;

    public static final String RESULT_KEY = "result";

    private ImageStreamMvp.Presenter presenter;

    private View bottomSheet, dismissArea;
    private RecyclerView imageList;
    private Toolbar toolbar;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.image_stream);
        bindViews();
        initToolbar();
        initBottomSheet();

        final ImageStreamMvp.Model model = new SomeModel(this.getApplicationContext());
        presenter = new ImageStreamPresenter(model, this);
        presenter.init();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSION_REQUEST_CODE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    presenter.permissionGranted(true);
                } else {
                    presenter.permissionGranted(false);
                    Toast.makeText(ImageStream.this, "No permission", Toast.LENGTH_LONG).show();
                }
                break;
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Belvedere.from(this).getFilesFromActivityOnResult(requestCode, resultCode, data, new Callback<List<BelvedereResult>>() {
            @Override
            public void success(List<BelvedereResult> result) {
                finishWithResult(result);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.imagestream_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;

        } else if(item.getItemId() == R.id.image_stream_system_gallery) {
            openGallery();
            return true;

        } else {
            return super.onOptionsItemSelected(item);

        }
    }

    private void finishWithResult(List<BelvedereResult> belvedereResults) {
        Intent intent = ImageStream.this.getIntent();
        intent.putParcelableArrayListExtra(ImageStream.RESULT_KEY, new ArrayList<>(belvedereResults));
        ImageStream.this.setResult(RESULT_OK, intent);
        finish();
    }

    private void initToolbar() {
        toolbar.setNavigationIcon(R.drawable.ic_close_black_24dp);
        setSupportActionBar(toolbar);
        final ActionBar actionBar = getSupportActionBar();
        if(actionBar != null) {
            actionBar.setTitle("Photo library");
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    private void bindViews() {
        this.bottomSheet = findViewById(R.id.bottom_sheet);
        this.dismissArea = findViewById(R.id.dismiss_area);
        this.imageList = (RecyclerView) findViewById(R.id.image_list);
        this.toolbar = (Toolbar) findViewById(R.id.toolbar);
    }

    private void initBottomSheet() {
        UiUtils.dimStatusBar(this);
        UiUtils.hideToolbar(this);

        ViewCompat.setElevation(imageList, getResources().getDimensionPixelSize(R.dimen.bottom_sheet_elevation));

        final BottomSheetBehavior<View> from = BottomSheetBehavior.from(bottomSheet);
        from.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
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
                if(slideOffset >= offset) {
                    ViewCompat.setAlpha(toolbar, 1f - ((1f - slideOffset) / offset));
                    UiUtils.showToolbar(ImageStream.this);
                } else {
                    UiUtils.hideToolbar(ImageStream.this);
                }
            }
        });

        dismissArea.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        bottomSheet.setClickable(true);
    }

    @Override
    public boolean isPermissionGranted() {
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            return true;
        } else {
            return ContextCompat.checkSelfPermission(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
    }

    @Override
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public void askForPermission() {
        final String[] permissions = { Manifest.permission.READ_EXTERNAL_STORAGE };
        ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE);
    }

    @Override
    public void loadImageStream() {
        final ImageStreamAdapter adapter = new ImageStreamAdapter(this, presenter.getImages());
        final StaggeredGridLayoutManager staggeredGridLayoutManager =
                new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL);

        imageList.setHasFixedSize(true);
        imageList.setAdapter(adapter);
        imageList.setAnimation(null);

        // https://code.google.com/p/android/issues/detail?id=230295
        staggeredGridLayoutManager.setItemPrefetchEnabled(false);

        imageList.setLayoutManager(staggeredGridLayoutManager);
    }

    @Override
    public void loadMediaSelector() {
        final ImageStreamAdapter adapter = new ImageStreamAdapter(this);
        final StaggeredGridLayoutManager staggeredGridLayoutManager =
                new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL);

        imageList.setHasFixedSize(true);
        imageList.setAdapter(adapter);
        imageList.setAnimation(null);

        // https://code.google.com/p/android/issues/detail?id=230295
        staggeredGridLayoutManager.setItemPrefetchEnabled(false);

        imageList.setLayoutManager(staggeredGridLayoutManager);
    }

    @Override
    public void imagesSelected(List<Uri> uris) {
        Belvedere.from(this).resolveUris(uris, new Callback<List<BelvedereResult>>() {
            @Override
            public void success(List<BelvedereResult> result) {
                finishWithResult(result);
            }
        });
    }

    @Override
    public void openCamera() {
        Belvedere.from(this).camera().open(this);
    }

    @Override
    public void openGallery() {
        Belvedere.from(this).document().contentType("image/*").open(this);
    }
}