package com.zendesk.belvedere.ui;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetBehavior;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static java.security.AccessController.getContext;

public class MediaChooser extends AppCompatActivity {

    private View bottomSheet;
    private RecyclerView list;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.belvedere_media_chooser);

        bindViews();
        initBottomSheet();
        getImages(this);
    }

    void initBottomSheet() {
        UiUtils.hideToolbar(this);
        UiUtils.dimStatusBar(this);
        BottomSheetBehavior<View> viewBottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        viewBottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);

    }

    private void bindViews() {
        this.bottomSheet = findViewById(R.id.belvedere_chooser_bottomsheet);
        this.list = (RecyclerView) findViewById(R.id.belvedere_chooser_list);
    }


    private void getImages(Context context) {
        String[] projection = new String[]{
                MediaStore.Images.ImageColumns._ID,
                MediaStore.Images.ImageColumns.DATA,
                MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME,
                MediaStore.Images.ImageColumns.DATE_TAKEN,
                MediaStore.Images.ImageColumns.MIME_TYPE
        };

        Cursor cursor = null;
        List<Uri> images = new ArrayList<>();

        try {
            cursor = context
                    .getContentResolver()
                    .query(MediaStore.Images.Media.INTERNAL_CONTENT_URI, projection, null, null, MediaStore.Images.ImageColumns.DATE_TAKEN + " DESC");

            if (cursor != null && cursor.moveToFirst()) {
                //final ImageView imageView = (ImageView) findViewById(R.id.pictureView);

                final Uri imageUri = ContentUris.withAppendedId(
                        MediaStore.Images.Media.INTERNAL_CONTENT_URI,
                        cursor.getInt(cursor.getColumnIndex(MediaStore.Images.ImageColumns._ID)));
                images.add(imageUri);
                System.out.println(cursor.getString(0) + " " + cursor.getString(1) + " " + cursor.getString(2) + " " + cursor.getString(3) + " " + cursor.getString(4));
            }

        } finally {
            if(cursor != null) {
                cursor.close();
            }
        }
    }


    static class Adapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return null;
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {

        }

        @Override
        public int getItemCount() {
            return 100;
        }



    }
}
