package zendesk.belvedere;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.List;
import java.util.Map;

interface ImageStreamMvp {

    interface Model {

        List<Uri> getLatestImages();

        List<MediaIntent> getMediaIntent();

        boolean hasCameraIntent();

        boolean hasDocumentIntent();

        boolean hasGooglePhotosIntent();

        MediaIntent getCameraIntent();

        MediaIntent getDocumentIntent();

        MediaIntent getGooglePhotosIntent();

        void neverAskForPermissionAgain(String permission);

        boolean canAskForPermission(String permission);

        List<MediaResult> getSelectedImages();
    }

    interface View {

        void initUiComponents();

        boolean isPermissionGranted(String permission);

        void askForPermission(String permission);

        void showImageStream(List<Uri> images, List<MediaResult> selectedImages, boolean showCamera);

        void showList(MediaIntent cameraIntent, MediaIntent documentIntent);

        void showDocumentMenuItem(boolean visible);

        void showGooglePhotosMenuItem(boolean visible);

        void openMediaIntent(MediaIntent mediaIntent);

        void finishWithoutResult();

        void finishIfNothingIsLeft();

        void hideCameraOption();

    }

    interface Presenter {

        void init();

        void initMenu();

        void openCamera();

        void openGallery();

        void openGooglePhotos();
    }


    interface PermissionListener {

        void permissionResult(Map<String, Boolean> permissionResult, List<String> dontAskAgain);

//        void permissionGranted(boolean granted, String permission);
//        void dontAskForPermissionAgain(String permission);
    }

    class ViewState implements Parcelable {

        final int bottomSheetState;

        ViewState(int bottomSheetState) {
            this.bottomSheetState = bottomSheetState;
        }

        int getBottomSheetState() {
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
