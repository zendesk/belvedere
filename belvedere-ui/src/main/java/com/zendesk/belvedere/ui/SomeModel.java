package com.zendesk.belvedere.ui;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;

import com.zendesk.belvedere.MediaIntent;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

class SomeModel implements ImageStreamMvp.Model {

    private static final int MAX_IMAGES = 100;

    private final Context context;
    private final BelvedereSharedPreferences belvedereSharedPreferences;
    private final List<MediaIntent> mediaIntents;

    SomeModel(Context context, Bundle bundle, BelvedereSharedPreferences belvedereSharedPreferences) {
        this.context = context;
        this.belvedereSharedPreferences = belvedereSharedPreferences;
        this.mediaIntents = BelvedereUi.getMediaIntents(belvedereSharedPreferences, bundle);
    }

    @Override
    public List<Uri> getLatestImages() {
        final List<Uri> uris = new ArrayList<>();
        final String[] projection = new String[]{
                MediaStore.Images.ImageColumns._ID,
                MediaStore.Images.ImageColumns.DATA
        };

        final Cursor cursor = context.getContentResolver()
                .query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection, null,
                        null, MediaStore.Images.ImageColumns.DATE_TAKEN + " DESC LIMIT " + MAX_IMAGES);

        try {
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    String imageLocation = cursor.getString(1);
                    uris.add(Uri.fromFile(new File(imageLocation)));
                }
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return uris;
    }

    @Override
    public List<MediaIntent> getMediaIntent() {
        return mediaIntents;
    }

    @Override
    public boolean hasCameraIntent() {
        return getCameraIntent() != null;
    }

    @Override
    public boolean hasDocumentIntent() {
        return getDocumentIntent() != null;
    }

    @Override
    public MediaIntent getCameraIntent() {
        return getIntentWithTarget(MediaIntent.TARGET_CAMERA);
    }

    @Override
    public MediaIntent getDocumentIntent() {
        return getIntentWithTarget(MediaIntent.TARGET_DOCUMENT);
    }

    private MediaIntent getIntentWithTarget(int target) {
        for (MediaIntent mediaIntent : mediaIntents) {
            if (mediaIntent.getTarget() == target) {
                return mediaIntent;
            }
        }

        return null;
    }

}
