package com.zendesk.belvedere.ui;


import android.net.Uri;

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
        if(view.isPermissionGranted()) {
            view.loadImageStream();
        } else {
            view.askForPermission();
        }
    }

    @Override
    public void permissionGranted(boolean granted) {
        if(granted) {
            view.loadImageStream();
        } else {
            view.loadMediaSelector();
        }
    }

    @Override
    public List<Uri> getImages() {
        return model.queryLatestImages();
    }
}
