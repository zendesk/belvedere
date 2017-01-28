package com.example.belvedere;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.GridLayout;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.display.FadeInBitmapDisplayer;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;
import com.zendesk.belvedere.Belvedere;
import com.zendesk.belvedere.BelvedereResult;
import com.zendesk.belvedere.Callback;
import com.zendesk.belvedere.MediaIntent;
import com.zendesk.belvedere.ui.BelvedereUi;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity {

    private static final int IMAGE_STREAM = 123;

    private static final String BELVEDERE_IMG_URL = "https://upload.wikimedia.org/wikipedia/commons/f/f8/Belvedere-wien.jpg";
    private static final String BELVEDERE_FILE_NAME = "belvedere.jpg";

    private Callback<List<BelvedereResult>> belvedereResult;

    @BindView(R.id.sample_belvedere_multiple)
    SwitchCompat switchMultiple;
    @BindView(R.id.sample_belvedere_camera)
    SwitchCompat switchCamera;
    @BindView(R.id.sample_belvedere_gallery)
    SwitchCompat switchGallery;
    @BindView(R.id.sample_belvedere_banner)
    ImageView banner;
    @BindView(R.id.sample_belvedere_toolbar)
    Toolbar toolbar;
    @BindView(R.id.collapsing_toolbar)
    CollapsingToolbarLayout collapsingToolbar;
    @BindView(R.id.sample_belvedere_gridlayout)
    GridLayout gridLayout;

    @BindView(R.id.sample_belvedere_btn_document)
    Button documentButton;
    @BindView(R.id.sample_belvedere_btn_camera)
    Button cameraButton;

    @BindView(R.id.main_content)
    CoordinatorLayout coordinatorLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        setSupportActionBar(toolbar);

        initBanner();

        documentButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                BelvedereUi.showDialog(getSupportFragmentManager(), getMediaIntents());
            }
        });

        cameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                BelvedereUi.startImageStream(MainActivity.this, getMediaIntents());
            }
        });

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (belvedereResult != null) {
            belvedereResult.cancel();
            belvedereResult = null;
        }
    }

    private List<MediaIntent> getMediaIntents() {
        final Belvedere from = Belvedere.from(MainActivity.this);
        final List<MediaIntent> mediaIntents = new ArrayList<>();

        if (switchCamera.isChecked()) {
            mediaIntents.add(from.camera().build());
        }

        if (switchGallery.isChecked()) {
            mediaIntents.add(from.document().contentType("image/*").allowMultiple(switchMultiple.isChecked()).build());
        }

        return mediaIntents;
    }

    private void initBanner() {
        collapsingToolbar.setStatusBarScrimColor(getResources().getColor(android.R.color.transparent));

        ImageLoader.getInstance()
                .displayImage(BELVEDERE_IMG_URL,
                        banner,
                        new SimpleImageLoadingListener() {
                            @Override
                            public void onLoadingComplete(String imageUri, View view, final Bitmap loadedImage) {
                                super.onLoadingComplete(imageUri, view, loadedImage);

                                Palette.from(loadedImage).generate(new Palette.PaletteAsyncListener() {
                                    @Override
                                    public void onGenerated(Palette palette) {
                                        final int primary = getResources().getColor(R.color.colorPrimary);

                                        final int darkVibrantColor = palette.getDarkVibrantColor(primary);
                                        final float[] hsv = new float[3];
                                        Color.colorToHSV(darkVibrantColor, hsv);
                                        hsv[2] *= 0.4f; // value component
                                        final int darkVibrantStatusBar = Color.HSVToColor(hsv);

                                        collapsingToolbar.setContentScrimColor(darkVibrantColor);
                                        collapsingToolbar.setStatusBarScrimColor(darkVibrantStatusBar);

                                        initBannerListener(loadedImage);
                                    }
                                });
                            }
                        });
    }

    private void initBannerListener(final Bitmap bitmap) {
        final BelvedereResult file = Belvedere.from(MainActivity.this).getFile(BELVEDERE_FILE_NAME);
        if (file == null) {
            return;
        }

        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(@NonNull final Void... params) {

                if (file.getFile().exists() && file.getFile().length() > 0) {
                    return null;
                }

                FileOutputStream out = null;
                try {
                    out = new FileOutputStream(file.getFile());
                    // Quality value will be ignored for PNG
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        if (out != null) {
                            out.close();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                return null;
            }

            @Override
            protected void onPostExecute(final Void aVoid) {
                super.onPostExecute(aVoid);
                setListenerToImageView(banner, file.getUri());
            }
        }.execute();
    }

    private void setListenerToImageView(ImageView imageView, final Uri uri) {
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                startActivity(Belvedere.from(MainActivity.this).getViewIntent(uri, "image/*"));
            }
        });

        imageView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(final View v) {
                startActivity(Belvedere.from(MainActivity.this).getShareIntent(uri, "image/*"));
                return true;
            }
        });
    }

    private void displayImages(List<BelvedereResult> belvedereResults) {
        final DisplayImageOptions build = new DisplayImageOptions.Builder()
                .cacheInMemory(false)
                .cacheOnDisk(false)
                .displayer(new FadeInBitmapDisplayer(1000))
                .build();

        final int imageSize = getResources().getDimensionPixelSize(R.dimen.sample_belvedere_image_size);

        for (final BelvedereResult r : belvedereResults) {
            final ImageView imageView = new ImageView(MainActivity.this);
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            imageView.setLayoutParams(new GridLayout.LayoutParams(new ViewGroup.LayoutParams(imageSize, imageSize)));
            gridLayout.addView(imageView);

            ImageLoader.getInstance()
                    .displayImage(r.getUri().toString(), imageView, build);

            setListenerToImageView(imageView, r.getUri());
        }
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        this.belvedereResult = new Callback<List<BelvedereResult>>() {
            @Override
            public void success(final List<BelvedereResult> belvedereResults) {
                displayImages(belvedereResults);
            }
        };

        Belvedere.from(this)
                .getFilesFromActivityOnResult(requestCode, resultCode, data, belvedereResult);
    }

}
