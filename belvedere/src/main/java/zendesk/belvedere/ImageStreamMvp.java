package zendesk.belvedere;

import android.view.View.OnClickListener;

import java.util.List;

interface ImageStreamMvp {

    interface Model {

        /**
         * Gets a list of the latest images. This API targets Android Q because of a difference
         * in how media queries are handled in newer Android versions. It will work on all API
         * levels.
         *
         * @return A list of the latest images
         */
        List<MediaResult> getLatestImages();

        boolean hasCameraIntent();

        boolean hasDocumentIntent();

        boolean hasGooglePhotosIntent();

        MediaIntent getCameraIntent();

        MediaIntent getDocumentIntent();

        MediaIntent getGooglePhotosIntent();

        List<MediaResult> getSelectedMediaResults();

        List<MediaResult> addToSelectedItems(MediaResult mediaResult);

        List<MediaResult> removeFromSelectedItems(MediaResult mediaResult);

        long getMaxFileSize();

        boolean showFullScreenOnly();

    }

    interface View {

        void initViews(boolean fullScreenOnly);

        void showImageStream(List<MediaResult> images, List<MediaResult> selectedImages, boolean fullScreenOnly, boolean showCamera, ImageStreamAdapter.Listener listener);

        void showDocumentMenuItem(OnClickListener onClickListener);

        void showGooglePhotosMenuItem(OnClickListener onClickListener);

        void openMediaIntent(MediaIntent mediaIntent, ImageStream imageStream);

        void showToast(int textId);

        void updateToolbarTitle(int selectedImages);

        boolean shouldShowFullScreen();

    }

    interface Presenter {

        void init();

        void onImageStreamScrolled(int height, int scrollArea, float scrollPosition);

        void dismiss();

    }

}
