package zendesk.belvedere;


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
        view.updateToolbarTitle(model.getSelectedImages().size());
    }

    @Override
    public void initMenu() {
        view.showGooglePhotosMenuItem(model.hasGooglePhotosIntent());
        view.showDocumentMenuItem(model.hasDocumentIntent());
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

    public List<MediaResult> setItemSelected(MediaResult mediaResult, boolean isSelected) {
        final List<MediaResult> mediaResults;

        if(isSelected) {
            mediaResults = model.addToSelectedItems(mediaResult);
        } else{
            mediaResults = model.removeFromSelectedItems(mediaResult);
        }

        view.updateToolbarTitle(mediaResults.size());
        return mediaResults;
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