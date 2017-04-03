package zendesk.belvedere;


import android.net.Uri;

import java.util.List;

class ImageStreamPresenter implements ImageStreamMvp.Presenter {

    private final ImageStreamMvp.Model model;
    private final ImageStreamMvp.View view;

    private final ImageStreamDataSource imageStreamDataSource;

    ImageStreamPresenter(ImageStreamMvp.Model model, ImageStreamMvp.View view, ImageStreamDataSource imageStreamDataSource) {
        this.model = model;
        this.view = view;
        this.imageStreamDataSource = imageStreamDataSource;
    }

    @Override
    public void init() {
        presentStream();
        initMenu();
    }

    @Override
    public void initMenu() {
        view.showDocumentMenuItem(model.hasDocumentIntent());
        view.showGooglePhotosMenuItem(model.hasGooglePhotosIntent());
    }

    @Override
    public void openCamera() {
        if (model.hasCameraIntent()) {
            view.openMediaIntent(model.getCameraIntent());
        }
    }

    @Override
    public void openGallery() {
        if (model.hasDocumentIntent()) {
            view.openMediaIntent(model.getDocumentIntent());
        }
    }

    @Override
    public void openGooglePhotos() {
        if (model.hasGooglePhotosIntent()) {
            view.openMediaIntent(model.getGooglePhotosIntent());
        }
    }

    public void setItemSelected(MediaResult mediaResult, boolean b) {
        if(b) {
            model.addToSelectedItems(mediaResult);
        } else{
            model.removeFromSelectedItems(mediaResult);
        }
    }

    @Override
    public List<MediaResult> getSelectedItems() {
        return model.getSelectedImages();
    }

    @Override
    public List<MediaIntent> getMediaIntents() {
        return model.getMediaIntent();
    }

    private void presentStream() {
        final List<MediaResult> latestImages = model.getLatestImages();
        final List<MediaResult> selectedImages = model.getSelectedImages();
        view.initUiComponents();
        view.showImageStream(latestImages, selectedImages, model.hasCameraIntent());
    }

}