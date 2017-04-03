package zendesk.belvedere;

import java.util.List;

interface ImageStreamMvp {

    interface Model {

        List<MediaResult> getLatestImages();

        List<MediaIntent> getMediaIntent();

        boolean hasCameraIntent();

        boolean hasDocumentIntent();

        boolean hasGooglePhotosIntent();

        MediaIntent getCameraIntent();

        MediaIntent getDocumentIntent();

        MediaIntent getGooglePhotosIntent();

        void neverAskForPermissionAgain(String permission);

        boolean canAskForPermission(String permission);

        List<MediaResult> getSelectedImages();

        void addToSelectedItems(MediaResult mediaResult);

        void removeFromSelectedItems(MediaResult mediaResult);

    }

    interface View {

        void initUiComponents();

        void showImageStream(List<MediaResult> images, List<MediaResult> selectedImages, boolean showCamera);

        void showDocumentMenuItem(boolean visible);

        void showGooglePhotosMenuItem(boolean visible);

        void openMediaIntent(MediaIntent mediaIntent);

    }

    interface Presenter {

        void init();

        void initMenu();

        void openCamera();

        void openGallery();

        void openGooglePhotos();

        void setItemSelected(MediaResult uri, boolean b);

        List<MediaResult> getSelectedItems();

        List<MediaIntent> getMediaIntents();

    }

}
