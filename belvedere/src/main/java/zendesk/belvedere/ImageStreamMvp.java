package zendesk.belvedere;

import android.view.View.OnClickListener;

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

        List<MediaResult> getSelectedMediaResults();

        List<MediaResult> addToSelectedItems(MediaResult mediaResult);

        List<MediaResult> removeFromSelectedItems(MediaResult mediaResult);

        long getMaxFileSize();

    }

    interface View {

        void initViews();

        void showImageStream(List<MediaResult> images, List<MediaResult> selectedImages, boolean showCamera, ImageStreamAdapter.Listener listener);

        void showDocumentMenuItem(OnClickListener onClickListener);

        void showGooglePhotosMenuItem(OnClickListener onClickListener);

        void openMediaIntent(MediaIntent mediaIntent, ImageStream imageStream);

        void showToast(int textId);

        void updateToolbarTitle(int selectedImages);

    }

    interface Presenter {

        void init();

        void onImageStreamScrolled(int height, int scrollArea, float scrollPosition);

        void dismiss();

    }

}
