package com.example.belvedere;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayout;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import zendesk.belvedere.Belvedere;
import zendesk.belvedere.BelvedereUi;
import zendesk.belvedere.Callback;
import zendesk.belvedere.MediaIntent;
import zendesk.belvedere.MediaResult;

public class MainActivity extends AppCompatActivity {

    private Callback<List<MediaResult>> belvedereResult;

    @BindView(R.id.sample_belvedere_multiple)
    SwitchCompat switchMultiple;
    @BindView(R.id.sample_belvedere_camera)
    SwitchCompat switchCamera;
    @BindView(R.id.sample_belvedere_gallery)
    SwitchCompat switchGallery;
    @BindView(R.id.sample_belvedere_toolbar)
    Toolbar toolbar;
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

        documentButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                BelvedereUi.showDialog(getSupportFragmentManager(), getMediaIntents());
            }
        });

        cameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                BelvedereUi
                        .imageStream(MainActivity.this)
                        .withCameraIntent()
                        .withDocumentIntent("*/*", true)
                        .show(MainActivity.this);
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
            mediaIntents.add(from.document().contentType("*/*").allowMultiple(switchMultiple.isChecked()).build());
        }

        return mediaIntents;
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

    private void displayImages(List<MediaResult> belvedereResults) {
        final int imageSize = getResources().getDimensionPixelSize(R.dimen.sample_belvedere_image_size);

        for (final MediaResult r : belvedereResults) {
            final ImageView imageView = new ImageView(MainActivity.this);
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            imageView.setLayoutParams(new GridLayout.LayoutParams(new ViewGroup.LayoutParams(imageSize, imageSize)));
            gridLayout.addView(imageView);

            Picasso.with(this)
                    .load(r.getUri())
                    .resize(imageSize, 0)
                    .into(imageView);
            setListenerToImageView(imageView, r.getUri());
        }
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Belvedere.from(this)
                .getFilesFromActivityOnResult(requestCode, resultCode, data, new Callback<List<MediaResult>>() {
                    @Override
                    public void success(List<MediaResult> result) {
                        displayImages(result);
                    }
                });
    }

}
