package zendesk.belvedere;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.webkit.MimeTypeMap;

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
    public List<MediaResult> getLatestImages() {
        final List<MediaResult> mediaResults = queryRecentImages();
        final List<MediaResult> userProvidedResults = mergeMediaResultLists(startConfig.getExtraItems(), startConfig.getSelectedItems());
        return mergeMediaResultLists(mediaResults, userProvidedResults);
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
    public List<MediaResult> getSelectedImages() {
        return selectedImages;
    }

    @Override
    public List<MediaResult> addToSelectedItems(MediaResult mediaResult) {
        selectedImages.add(mediaResult);
        return selectedImages;
    }

    @Override
    public List<MediaResult> removeFromSelectedItems(MediaResult mediaResult) {
        selectedImages.remove(mediaResult);
        return selectedImages;
    }

    private List<MediaResult> queryRecentImages() {
        final List<MediaResult> mediaResults = new ArrayList<>();

        final String[] projection = new String[]{
                MediaStore.Images.ImageColumns._ID,
                MediaStore.MediaColumns.DISPLAY_NAME,
                MediaStore.MediaColumns.SIZE
        };

        final Cursor cursor = context.getContentResolver()
                .query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection, null,
                        null, MediaStore.Images.ImageColumns.DATE_TAKEN + " DESC LIMIT " + MAX_IMAGES);

        try {
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    final Uri uri = MediaStore.Files.getContentUri("external",
                            cursor.getLong(cursor.getColumnIndex(MediaStore.Images.ImageColumns._ID)));

                    final long size = cursor.getLong(cursor.getColumnIndex(MediaStore.MediaColumns.SIZE));
                    final String name = cursor.getString(cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME));

                    final int index = name.lastIndexOf(".");
                    String mimeType = "image/jpeg";
                    if(index != -1) {
                        mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(name.substring(index + 1));
                    }

                    mediaResults.add(new MediaResult(null, uri, uri, name, mimeType, size));
                }
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return mediaResults;
    }

    private List<MediaResult> mergeMediaResultLists(List<MediaResult> images, List<MediaResult> toMerge) {
        final List<MediaResult> mediaResults = new ArrayList<>(images.size() + toMerge.size());
        mediaResults.addAll(images);

        for(MediaResult mediaResult : toMerge) {

           boolean contains = false;
            for(MediaResult m : images){
                if(m.getOriginalUri().equals(mediaResult.getOriginalUri())) {
                    contains = true;
                    break;
                }
            }

            if(!contains) {
                mediaResults.add(0, mediaResult);
            }
        }

        return mediaResults;
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
