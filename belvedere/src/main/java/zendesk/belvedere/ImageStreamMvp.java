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

        BelvedereUi.UiConfig getUiConfig();

    }

    interface View {

        void initViews(List<Integer> touchableItemIds);

        void showImageStream(List<MediaResult> images, List<MediaResult> selectedImages, boolean showCamera, ImageStreamAdapter.Listener listener);

        void showDocumentMenuItem(android.view.View.OnClickListener onClickListener);

        void showGooglePhotosMenuItem(android.view.View.OnClickListener onClickListener);

        void openMediaIntent(MediaIntent mediaIntent, ImageStream imageStream);

        void showToast(int textId);

        void updateToolbarTitle(int selectedImages);

    }

    interface Presenter {

        void init();

        void initMenu();

        void onImageStreamScrolled(int height, int scrollArea, float scrollPosition);

        void dismiss();

        List<MediaResult> setItemSelected(MediaResult mediaResult, boolean isSelected);

    }

}
