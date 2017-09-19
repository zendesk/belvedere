package zendesk.belvedere;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;

class ImageStreamModel implements ImageStreamMvp.Model {

    private static final String GOOGLE_PHOTOS_PACKAGE_NAME = "com.google.android.apps.photos";
    private static final int MAX_IMAGES = 500;

    private final ImageStreamService imageStreamProvider;
    private final PermissionStorage preferences;

    private final BelvedereUi.UiConfig startConfig;
    private List<MediaIntent> mediaIntents;

    private final List<MediaResult> selectedImages;

    ImageStreamModel(Context context, BelvedereUi.UiConfig startConfig, PermissionStorage preferences) {
        this.imageStreamProvider = new ImageStreamService(context);
        this.preferences = preferences;
        this.startConfig = startConfig;
        this.mediaIntents = filterIntents(startConfig.getIntents());
        this.selectedImages = startConfig.getSelectedItems();
    }

    @Override
    public List<MediaResult> getLatestImages() {
        final List<MediaResult> mediaResults = imageStreamProvider.queryRecentImages(MAX_IMAGES);
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
        return getDocumentIntent() != null && imageStreamProvider.isAppAvailable(GOOGLE_PHOTOS_PACKAGE_NAME);
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

    @Override
    public BelvedereUi.UiConfig getUiConfig() {
        return startConfig;
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
