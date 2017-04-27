package zendesk.belvedere;

import java.util.List;

interface ImageStreamMvp {

    interface Model {

        List<MediaResult> getLatestImages();

        boolean hasCameraIntent();

        boolean hasDocumentIntent();

        boolean hasGooglePhotosIntent();

        MediaIntent getCameraIntent();

        MediaIntent getDocumentIntent();

        MediaIntent getGooglePhotosIntent();

        List<MediaResult> getSelectedImages();

        List<MediaResult> addToSelectedItems(MediaResult mediaResult);

        List<MediaResult> removeFromSelectedItems(MediaResult mediaResult);

    }

    interface View {

        void initUiComponents();

        void updateToolbarTitle(int selectedImages);

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

        List<MediaResult> setItemSelected(MediaResult uri, boolean b);

    }

}
