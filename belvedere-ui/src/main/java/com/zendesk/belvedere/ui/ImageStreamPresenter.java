package com.zendesk.belvedere.ui;


import android.Manifest;
import android.net.Uri;
import android.os.Build;

import java.util.List;

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

        if(isBelowKitkat || hasReadPermission) {
            view.showImageStream(model.getLatestImages(), model.hasCameraIntent());

        } else {
            view.askForPermission(Manifest.permission.READ_EXTERNAL_STORAGE);

        }
    }

    @Override
    public void initMenu() {
        view.showDocumentMenuItem(model.hasDocumentIntent());
    }

    @Override
    public void permissionGranted(boolean granted, String permission) {
        switch (permission) {
            case Manifest.permission.READ_EXTERNAL_STORAGE:
                if(granted) {
                    view.showImageStream(model.getLatestImages(), model.hasCameraIntent());
                } else {
                    if(model.hasCameraIntent() && model.hasDocumentIntent()) {
                        view.showList(model.getCameraIntent(), model.getDocumentIntent());

                    } else if(model.hasCameraIntent()) {
                        // view.open(model.getCameraIntent());

                    } else if(model.hasDocumentIntent()){
                        // view.showList(model.getDocumentIntent());

                    } else {
                        // view.finish();
                    }
                }
                break;
            case Manifest.permission.CAMERA:
                break;
        }
    }

    @Override
    public List<Uri> getImages() {
        return null;
    }
}
