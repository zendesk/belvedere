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

    public void setItemSelected(Uri uri, boolean b) {
        final MediaResult mediaResult = new MediaResult(null, uri, uri);
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

    private void presentStream() {
        final List<Uri> latestImages = model.getLatestImages();
        final List<MediaResult> selectedImages = model.getSelectedImages();
        if(latestImages.size() > 0) {
            view.initUiComponents();
            view.showImageStream(latestImages, selectedImages, model.hasCameraIntent());
        } else {
            presentList();
        }
    }

    private void presentList() {
        if (model.hasCameraIntent() && model.hasDocumentIntent()) {
            view.initUiComponents();
            view.showList(model.getCameraIntent(), model.getDocumentIntent());

        } else if (model.hasCameraIntent()) {
            openCamera();

        } else if (model.hasDocumentIntent()) {
            openGallery();

        } else {
            view.finishWithoutResult();
        }
    }
}