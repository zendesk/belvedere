package com.example.belvedere;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.GridLayout;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.display.FadeInBitmapDisplayer;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import zendesk.belvedere.Belvedere;
import zendesk.belvedere.BelvedereCallback;
import zendesk.belvedere.BelvedereResult;
import zendesk.belvedere.BelvedereSource;

public class MainActivity extends AppCompatActivity {

    private static final String BELVEDERE_IMG_URL = "https://upload.wikimedia.org/wikipedia/commons/f/f8/Belvedere-wien.jpg";
    private static final String BELVEDERE_FILE_NAME = "belvedere.jpg";

    private BelvedereCallback<List<BelvedereResult>> belvedereResult;
    private HeadLessFragment dataFragment;

    @BindView(R.id.sample_belvedere_multiple) SwitchCompat switchMultiple;
    @BindView(R.id.sample_belvedere_logging) SwitchCompat switchLogging;
    @BindView(R.id.sample_belvedere_camera) SwitchCompat switchCamera;
    @BindView(R.id.sample_belvedere_gallery) SwitchCompat switchGallery;
    @BindView(R.id.sample_belvedere_banner) ImageView banner;
    @BindView(R.id.sample_belvedere_toolbar) Toolbar toolbar;
    @BindView(R.id.collapsing_toolbar) CollapsingToolbarLayout collapsingToolbar;
    @BindView(R.id.sample_belvedere_log) TextView log;
    @BindView(R.id.sample_belvedere_gridlayout) GridLayout gridLayout;
    @BindView(R.id.sample_belvedere_dialog) Button chooserButton;
    @BindView(R.id.main_content) CoordinatorLayout coordinatorLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        setSupportActionBar(toolbar);
        dataFragment = initHeadlessFragment();
        if(dataFragment.getBelvedere() == null){
            dataFragment.setBelvedere(initBelvedere());
        }

        initBanner();

        chooserButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                dataFragment.getBelvedere().showDialog(getSupportFragmentManager());
            }
        });

        switchMultiple.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                dataFragment.setBelvedere(initBelvedere());
            }
        });

        switchLogging.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                dataFragment.setBelvedere(initBelvedere());
            }
        });


        switchGallery.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                if(!switchCamera.isChecked() && !isChecked){
                    Snackbar.make(coordinatorLayout, "At least one source must be selected", Snackbar.LENGTH_LONG).show();
                    switchGallery.setChecked(true);
                }else {
                    dataFragment.setBelvedere(initBelvedere());
                }
            }
        });

        switchCamera.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                if(!switchGallery.isChecked() && !isChecked){
                    Snackbar.make(coordinatorLayout, "At least one source must be selected", Snackbar.LENGTH_LONG).show();
                    switchCamera.setChecked(true);
                } else {
                    dataFragment.setBelvedere(initBelvedere());
                }
            }
        });

    }

    private HeadLessFragment initHeadlessFragment(){
        final FragmentManager fm = getSupportFragmentManager();
        final Fragment fragment = fm.findFragmentByTag(HeadLessFragment.TAG);

        if(fragment instanceof HeadLessFragment){
            return (HeadLessFragment)fragment;
        } else {
            final HeadLessFragment headLessFragment = new HeadLessFragment();
            fm.beginTransaction()
                    .add(headLessFragment, HeadLessFragment.TAG)
                    .commit();
            return headLessFragment;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(belvedereResult != null){
            belvedereResult.cancel();
            belvedereResult = null;
        }
    }

    private Belvedere initBelvedere(){

        final List<BelvedereSource> belvedereSources = new ArrayList<>();
        if(switchCamera.isChecked()){
            belvedereSources.add(BelvedereSource.Camera);
        }

        if(switchGallery.isChecked()){
            belvedereSources.add(BelvedereSource.Gallery);
        }

        return Belvedere.from(this)
                .withContentType("image/*")
                .withAllowMultiple(switchMultiple.isChecked())
                .withCustomLogger(new SampleLogger(log))
                .withDebug(switchLogging.isChecked())
                .withSource(belvedereSources.toArray(new BelvedereSource[belvedereSources.size()]))
                .build();
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

    private void initBannerListener(final Bitmap bitmap){
        final BelvedereResult file = dataFragment.getBelvedere().getFileRepresentation(BELVEDERE_FILE_NAME);
        if(file == null) return;

        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(@NonNull final Void... params) {

                if(file.getFile().exists() && file.getFile().length() > 0){
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
                        if(out != null){
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

    private void setListenerToImageView(ImageView imageView, final Uri uri){
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                final Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(uri, "image/*");
                dataFragment.getBelvedere().grantPermissionsForUri(intent, uri);
                MainActivity.this.startActivity(intent);
            }
        });

        imageView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(final View v) {
                final Intent shareIntent = new Intent();
                shareIntent.setAction(Intent.ACTION_SEND);
                shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
                shareIntent.setType("image/*");
                dataFragment.getBelvedere().grantPermissionsForUri(shareIntent, uri);
                startActivity(shareIntent);
                return true;
            }
        });
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        this.belvedereResult = new BelvedereCallback<List<BelvedereResult>>() {
            @Override
            public void success(final List<BelvedereResult> belvedereResults) {
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
        };

        dataFragment.getBelvedere().getFilesFromActivityOnResult(requestCode, resultCode, data, belvedereResult);
    }


    public static class HeadLessFragment extends Fragment {

        public static final String TAG = "HeadLessFragment";
        private Belvedere mBelvedere;

        @Nullable
        @Override
        public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
            setRetainInstance(true);
            return null;
        }

        public void setBelvedere(Belvedere belvedere){
            this.mBelvedere = belvedere;
        }

        public Belvedere getBelvedere(){
            return mBelvedere;
        }
    }
}
