package com.zendesk.belvedere.ui;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

class SomeModel implements ImageStreamMvp.Model{

    private static final int MAX_IMAGES = 100;

    private final Context context;

    SomeModel(Context context) {
        this.context = context;
    }

    @Override
    public List<Uri> queryLatestImages() {
        final List<Uri> uris = new ArrayList<>();
        final String[] projection = new String[]{
                MediaStore.Images.ImageColumns._ID,
                MediaStore.Images.ImageColumns.DATA
        };

        final Cursor cursor = context.getContentResolver()
                .query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection, null,
                        null, MediaStore.Images.ImageColumns.DATE_TAKEN + " DESC LIMIT " + MAX_IMAGES);

        try {
            if(cursor != null) {
                while (cursor.moveToNext()) {
                    String imageLocation = cursor.getString(1);
                    uris.add(Uri.fromFile(new File(imageLocation)));
                }
            }
        } finally {
            if(cursor != null) {
                cursor.close();
            }
        }

        return uris;
    }

}
