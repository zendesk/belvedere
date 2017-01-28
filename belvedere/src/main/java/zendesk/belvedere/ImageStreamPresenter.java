package zendesk.belvedere;


import android.Manifest;
import android.os.Build;
import android.text.TextUtils;

class ImageStreamPresenter implements ImageStreamMvp.Presenter {

    private final ImageStreamMvp.Model model;
    private final ImageStreamMvp.View view;

    ImageStreamPresenter(ImageStreamMvp.Model model, ImageStreamMvp.View view) {
        this.model = model;
        this.view = view;
    }

    @Override
    public void init() {
        final boolean isBelowKitkat = Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT;
        final boolean hasReadPermission = view.isPermissionGranted(Manifest.permission.READ_EXTERNAL_STORAGE);

        if (isBelowKitkat || hasReadPermission) {
            presentStream();
        } else if (model.canAskForPermission(Manifest.permission.READ_EXTERNAL_STORAGE)) {
            view.askForPermission(Manifest.permission.READ_EXTERNAL_STORAGE);
        } else {
            presentList();
        }
    }

    @Override
    public void initMenu() {
        view.showDocumentMenuItem(model.hasDocumentIntent());
    }

    @Override
    public void permissionGranted(boolean granted, String permission) {
        switch (permission) {
            case Manifest.permission.READ_EXTERNAL_STORAGE: {
                if (granted) {
                    presentStream();
                } else {
                    presentList();
                }
                break;
            }
            case Manifest.permission.CAMERA: {
                if (granted) {
                    view.openMediaIntent(model.getCameraIntent());
                } else {
                    view.finishIfNothingIsLeft();
                }
                break;
            }
        }
    }

    @Override
    public void dontAskForPermissionAgain(String permission) {
        model.dontAskForPermissionAgain(permission);

        if (Manifest.permission.READ_EXTERNAL_STORAGE.equals(permission)) {
            presentList();
        } else if (Manifest.permission.CAMERA.equals(permission)) {
            view.hideCameraOption();
        }
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

    private void presentStream() {
        view.initUiComponents();
        view.showImageStream(model.getLatestImages(), model.hasCameraIntent());
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