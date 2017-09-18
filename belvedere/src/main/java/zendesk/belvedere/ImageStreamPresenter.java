package zendesk.belvedere;


import android.view.View;

import java.util.List;

class ImageStreamPresenter implements ImageStreamMvp.Presenter{

    private final ImageStreamMvp.Model model;
    private final ImageStreamUi view;
    private final ImageStream imageStreamBackend;

    ImageStreamPresenter(ImageStreamMvp.Model model, ImageStreamUi view, ImageStream imageStreamBackend) {
        this.model = model;
        this.view = view;
        this.imageStreamBackend = imageStreamBackend;
    }

    @Override
    public void init() {
        presentStream();
        initMenu();
        view.updateToolbarTitle(model.getSelectedImages().size());
    }

    @Override
    public void initMenu() {
        if(model.hasGooglePhotosIntent()) {
            final View.OnClickListener clickListener = new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    ImageStreamPresenter.this.view.openMediaIntent(model.getGooglePhotosIntent(), imageStreamBackend);
                }
            };

            view.showGooglePhotosMenuItem(clickListener);
        }

        if(model.hasDocumentIntent()) {
            final View.OnClickListener clickListener = new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    ImageStreamPresenter.this.view.openMediaIntent(model.getDocumentIntent(), imageStreamBackend);
                }
            };

            view.showDocumentMenuItem(clickListener);
        }
    }

    @Override
    public void onImageStreamScrolled(int height, int scrollArea, float scrollPosition) {
        if(scrollPosition >= 0) {
            imageStreamBackend.notifyScrollListener(height, scrollArea, scrollPosition);
        }
    }

    @Override
    public void dismiss() {
        // null out references
        imageStreamBackend.setImageStreamUi(null, null);

        // reset animations
        view.tintStatusBar(0);
        imageStreamBackend.notifyScrollListener(0,0,0);

        // notify dismiss
        imageStreamBackend.notifyDismissed();
    }

    @Override
    public List<MediaResult> setItemSelected(MediaResult mediaResult, boolean isSelected) {
        final List<MediaResult> mediaResults;

        if(isSelected) {
            mediaResults = model.addToSelectedItems(mediaResult);
        } else{
            mediaResults = model.removeFromSelectedItems(mediaResult);
        }

        return mediaResults;
    }

    private void presentStream() {
        final List<MediaResult> latestImages = model.getLatestImages();
        final List<MediaResult> selectedImages = model.getSelectedImages();
        view.initViews(model.getUiConfig().getTouchableElements());
        view.showImageStream(latestImages, selectedImages, model.hasCameraIntent(), imageStreamListener);
        imageStreamBackend.notifyVisible();
    }

    private final ImageStreamAdapter.Listener imageStreamListener = new ImageStreamAdapter.Listener() {
        @Override
        public void onOpenCamera() {
            if (model.hasCameraIntent()) {
                view.openMediaIntent(model.getCameraIntent(), imageStreamBackend);
            }
        }

        @Override
        public void onSelectionChanged(ImageStreamItems.Item item, int position) {
            MediaResult media = item.getMediaResult();
            final BelvedereUi.UiConfig uiConfig = model.getUiConfig();

            if(media != null && media.getSize() <= uiConfig.getMaxFileSize() || uiConfig.getMaxFileSize() == -1L) {
                item.setSelected(!item.isSelected());

                List<MediaResult> items = setItemSelected(media, item.isSelected());

                view.updateToolbarTitle(items.size());

                imageStreamBackend.notifyImageSelected(items, true);
            } else {
                view.showToast(model.getUiConfig().getMaxSizeErrorMessage());
            }
        }
    };
}