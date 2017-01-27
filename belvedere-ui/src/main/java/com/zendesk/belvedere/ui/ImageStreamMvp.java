package com.zendesk.belvedere.ui;

import android.net.Uri;

import java.util.List;

interface ImageStreamMvp {

    interface Model {

        List<Uri> queryLatestImages();

    }

    interface View {

        boolean isPermissionGranted();

        void askForPermission();

        void loadImageStream();

        void loadMediaSelector();

    }

    interface Presenter {

        void init();

        void permissionGranted(boolean granted);

        List<Uri> getImages();

    }
}
