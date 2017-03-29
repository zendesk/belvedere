package zendesk.belvedere;


import android.net.Uri;
import android.text.TextUtils;

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
//        final boolean isBelowKitkat = Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT;
//        final boolean hasReadPermission = view.isPermissionGranted(Manifest.permission.READ_EXTERNAL_STORAGE);
//
//        if (isBelowKitkat || hasReadPermission) {
//            presentStream();
//
//        } else if (model.canAskForPermission(Manifest.permission.READ_EXTERNAL_STORAGE)) {
//            view.askForPermission(Manifest.permission.READ_EXTERNAL_STORAGE);
//
//        } else {
//            presentList();
//
//        }
    }

    @Override
    public void initMenu() {
        view.showDocumentMenuItem(model.hasDocumentIntent());
        view.showGooglePhotosMenuItem(model.hasGooglePhotosIntent());
    }

    @Override
    public void openCamera() {
        if (model.hasCameraIntent()) {
            final MediaIntent cameraIntent = model.getCameraIntent();
            if (TextUtils.isEmpty(cameraIntent.getPermission())) {
                view.openMediaIntent(cameraIntent);
            } else {
                view.askForPermission(cameraIntent.getPermission());
            }
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