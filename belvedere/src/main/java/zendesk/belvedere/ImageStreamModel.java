package zendesk.belvedere;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.annotation.VisibleForTesting;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

class ImageStreamModel implements ImageStreamMvp.Model {

    private static final String GOOGLE_PHOTOS_PACKAGE_NAME = "com.google.android.apps.photos";
    private static final int MAX_IMAGES = 500;

    private final ImageStreamService imageStreamService;
    private final List<MediaIntent> mediaIntents;
    private final List<MediaResult> selectedMediaResults;
    private final List<MediaResult> additionalMediaResults;
    private final long maxFileSize;
    private final boolean fullScreenOnly;

    ImageStreamModel(Context context, BelvedereUi.UiConfig startConfig) {
        this.imageStreamService = new ImageStreamService(context);
        this.mediaIntents = startConfig.getIntents();
        this.selectedMediaResults = startConfig.getSelectedItems();
        this.additionalMediaResults = startConfig.getExtraItems();
        this.maxFileSize = startConfig.getMaxFileSize();
        this.fullScreenOnly = startConfig.showFullScreenOnly();
    }

    @VisibleForTesting
    ImageStreamModel(ImageStreamService imageStreamService,
                     long maxFileSize,
                     List<MediaIntent> intents,
                     List<MediaResult> selectedMediaResults,
                     List<MediaResult> additionalMediaResults,
                     boolean fullScreenOnly) {
        this.imageStreamService = imageStreamService;
        this.maxFileSize = maxFileSize;
        this.mediaIntents = intents;
        this.selectedMediaResults = selectedMediaResults;
        this.additionalMediaResults = additionalMediaResults;
        this.fullScreenOnly = fullScreenOnly;
    }

    @Override
    public List<MediaResult> getLatestImages() {
        final List<MediaResult> mediaResults = imageStreamService.queryRecentImages(MAX_IMAGES);
        final List<MediaResult> userProvidedResults = mergeMediaResultLists(additionalMediaResults, selectedMediaResults);
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
        return getDocumentIntent() != null && imageStreamService.isAppAvailable(GOOGLE_PHOTOS_PACKAGE_NAME);
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
        final MediaIntent mediaIntent = getDocumentIntent();

        if(mediaIntent == null) {
            return null;
        }

        Intent intent = mediaIntent.getIntent();
        intent.setPackage(GOOGLE_PHOTOS_PACKAGE_NAME);
        intent.setAction(Intent.ACTION_GET_CONTENT);
        return mediaIntent;
    }

    @Override
    public List<MediaResult> getSelectedMediaResults() {
        return selectedMediaResults;
    }

    @Override
    public List<MediaResult> addToSelectedItems(MediaResult mediaResult) {
        selectedMediaResults.add(mediaResult);
        return selectedMediaResults;
    }

    @Override
    public List<MediaResult> removeFromSelectedItems(MediaResult mediaResult) {
        selectedMediaResults.remove(mediaResult);
        return selectedMediaResults;
    }

    @Override
    public long getMaxFileSize() {
        return maxFileSize;
    }

    @Override
    public boolean showFullScreenOnly() {
        return fullScreenOnly;
    }

    private List<MediaResult> mergeMediaResultLists(List<MediaResult> images, List<MediaResult> toMerge) {
        final HashSet<Uri> existingMedia = new HashSet<>(images.size());
        for(MediaResult m : images) {
            existingMedia.add(m.getOriginalUri());
        }

        final List<MediaResult> mediaResults = new ArrayList<>(images.size() + toMerge.size());
        mediaResults.addAll(images);

        for(int i = toMerge.size() - 1; i >= 0; i--) {
            final MediaResult mediaResult = toMerge.get(i);
            if(!existingMedia.contains(mediaResult.getOriginalUri())) {
                mediaResults.add(0, mediaResult);
            }
        }

        return mediaResults;
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
