package com.zendesk.belvedere.ui;

import android.net.Uri;

import com.zendesk.belvedere.MediaIntent;

import java.util.List;

interface ImageStreamMvp {

    interface Model {

        List<Uri> getLatestImages();

        List<MediaIntent> getMediaIntent();

        boolean hasCameraIntent();

        boolean hasDocumentIntent();

        MediaIntent getCameraIntent();

        MediaIntent getDocumentIntent();

    }

    interface View {

        boolean isPermissionGranted(String permission);

        void askForPermission(String permission);

        void showImageStream(List<Uri> images, boolean showCamera);

        void showList(MediaIntent cameraIntent, MediaIntent documentIntent);

        void showDocumentMenuItem(boolean visible);
    }

    interface Presenter {

        void init();

        void initMenu();

        void permissionGranted(boolean granted, String permission);

        List<Uri> getImages();

    }
}
