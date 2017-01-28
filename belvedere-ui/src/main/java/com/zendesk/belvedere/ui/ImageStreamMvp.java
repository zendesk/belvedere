package com.zendesk.belvedere.ui;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

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

        void dontAskForPermissionAgain(String permission);

        boolean canAskForPermission(String permission);
    }

    interface View {

        void initUiComponents();

        boolean isPermissionGranted(String permission);

        void askForPermission(String permission);

        void showImageStream(List<Uri> images, boolean showCamera);

        void showList(MediaIntent cameraIntent, MediaIntent documentIntent);

        void showDocumentMenuItem(boolean visible);

        void openMediaIntent(MediaIntent mediaIntent);

        void finishWithoutResult();

        void finishIfNothingIsLeft();

        void hideCameraOption();

    }

    interface Presenter {

        void init();

        void initMenu();

        void permissionGranted(boolean granted, String permission);

        void dontAskForPermissionAgain(String permission);

        void openCamera();

        void openGallery();

    }

    class ViewState implements Parcelable {

        final int bottomSheetState;

        ViewState(int bottomSheetState) {
            this.bottomSheetState = bottomSheetState;
        }

        public int getBottomSheetState() {
            return bottomSheetState;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(bottomSheetState);
        }

        ViewState(Parcel in) {
            bottomSheetState = in.readInt();
        }

        public static final Creator<ViewState> CREATOR = new Creator<ViewState>() {
            @Override
            public ViewState createFromParcel(Parcel in) {
                return new ViewState(in);
            }

            @Override
            public ViewState[] newArray(int size) {
                return new ViewState[size];
            }
        };
    }

}
