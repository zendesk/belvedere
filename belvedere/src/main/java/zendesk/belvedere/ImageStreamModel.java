package zendesk.belvedere;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.text.TextUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

class ImageStreamModel implements ImageStreamMvp.Model {

    private static final int MAX_IMAGES = 500;

    private final Context context;
    private final PermissionStorage preferences;

    private List<MediaIntent> mediaIntents;

    ImageStreamModel(Context context, List<MediaIntent> mediaIntents,
                     PermissionStorage preferences) {
        this.context = context;
        this.preferences = preferences;
        this.mediaIntents = filterIntents(mediaIntents);
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

    @Override
    public void dontAskForPermissionAgain(String permission) {
        preferences.neverEverAskForThatPermissionAgain(permission);
        mediaIntents = filterIntents(mediaIntents);
    }

    @Override
    public boolean canAskForPermission(String permission) {
        return !preferences.shouldINeverEverAskForThatPermissionAgain(permission);
    }

    private List<MediaIntent> filterIntents(List<MediaIntent> mediaIntents) {
        List<MediaIntent> filter = new ArrayList<>();
        for (MediaIntent intent : mediaIntents) {
            if (TextUtils.isEmpty(intent.getPermission())
                    || !preferences.shouldINeverEverAskForThatPermissionAgain(intent.getPermission())) {
                filter.add(intent);
            }
        }
        return filter;
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
