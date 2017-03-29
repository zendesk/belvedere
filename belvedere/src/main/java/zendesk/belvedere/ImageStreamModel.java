package zendesk.belvedere;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;

class ImageStreamModel implements ImageStreamMvp.Model {

    private static final String GOOGLE_PHOTOS_PACKAGE_NAME = "com.google.android.apps.photos";
    private static final int MAX_IMAGES = 500;

    private final Context context;
    private final PermissionStorage preferences;

    private final BelvedereUi.UiConfig startConfig;
    private List<MediaIntent> mediaIntents;

    private final List<MediaResult> selectedImages;

    ImageStreamModel(Context context,
                     BelvedereUi.UiConfig startConfig,
                     PermissionStorage preferences) {
        this.context = context;
        this.preferences = preferences;
        this.startConfig = startConfig;
        this.mediaIntents = filterIntents(startConfig.getIntents());
        this.selectedImages = startConfig.getSelectedItems();
    }

    @Override
    public List<Uri> getLatestImages() {
        final List<Uri> uris = new ArrayList<>();
        final String[] projection = new String[]{
                MediaStore.Images.ImageColumns._ID
        };

        final Cursor cursor = context.getContentResolver()
                .query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection, null,
                        null, MediaStore.Images.ImageColumns.DATE_TAKEN + " DESC LIMIT " + MAX_IMAGES);

        try {
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    final Uri uri = MediaStore.Files.getContentUri("external",
                            cursor.getLong(cursor.getColumnIndex(MediaStore.Images.ImageColumns._ID)));
                    uris.add(uri);
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
    public boolean hasGooglePhotosIntent() {
        return getDocumentIntent() != null && Utils.isAppAvailable(GOOGLE_PHOTOS_PACKAGE_NAME, context);
    }

    @Override
    public MediaIntent getCameraIntent() {
        return getIntentForTarget(MediaIntent.TARGET_CAMERA);
    }

    @Override
    public MediaIntent getDocumentIntent() {
        return getIntentForTarget(MediaIntent.TARGET_DOCUMENT);
    }

    @Override
    public MediaIntent getGooglePhotosIntent() {
        MediaIntent mediaIntent = getDocumentIntent();
        Intent intent = mediaIntent.getIntent();
        intent.setPackage(GOOGLE_PHOTOS_PACKAGE_NAME);
        intent.setAction(Intent.ACTION_GET_CONTENT);
        return mediaIntent;
    }

    @Override
    public void neverAskForPermissionAgain(String permission) {
        preferences.neverEverAskForThatPermissionAgain(permission);
        mediaIntents = filterIntents(mediaIntents);
    }

    @Override
    public boolean canAskForPermission(String permission) {
        return !preferences.shouldINeverEverAskForThatPermissionAgain(permission);
    }

    @Override
    public List<MediaResult> getSelectedImages() {
        return selectedImages;
    }

    @Override
    public void addToSelectedItems(MediaResult mediaResult) {
        selectedImages.add(mediaResult);
    }

    @Override
    public void removeFromSelectedItems(MediaResult mediaResult) {
        selectedImages.remove(mediaResult);
    }

    private List<MediaIntent> filterIntents(List<MediaIntent> mediaIntents) {
        List<MediaIntent> filter = new ArrayList<>();
        for (MediaIntent intent : mediaIntents) {
            if (TextUtils.isEmpty(intent.getPermission())
                    || !preferences.shouldINeverEverAskForThatPermissionAgain(intent.getPermission())
                    || intent.isAvailable()) {
                filter.add(intent);
            }
        }
        return filter;
    }

    private MediaIntent getIntentForTarget(int target) {
        for (MediaIntent mediaIntent : mediaIntents) {
            if (mediaIntent.getTarget() == target) {
                return mediaIntent;
            }
        }

        return null;
    }

}
