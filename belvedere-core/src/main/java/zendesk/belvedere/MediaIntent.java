package zendesk.belvedere;

import android.app.Activity;
import android.content.Intent;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.util.Pair;

public class MediaIntent implements Parcelable {

    final static int TARGET_DOCUMENT = 1;
    final static int TARGET_CAMERA = 2;

    static MediaIntent notAvailable() {
        return new MediaIntent(-1, null, null, false, -1);
    }

    private final boolean isAvailable;
    private final int requestCode;
    private final Intent intent;
    private final String permission;
    private final int target;

    MediaIntent(int requestCode, Intent intent, String permission, boolean isAvailable, int target) {
        this.requestCode = requestCode;
        this.intent = intent;
        this.permission = permission;
        this.isAvailable = isAvailable;
        this.target = target;
    }

    public void open(Fragment fragment) {
        fragment.startActivityForResult(intent, requestCode);
    }

    public void open(Activity activity) {
        activity.startActivityForResult(intent, requestCode);
    }

    public String getPermission() {
        return permission;
    }

    public Intent getIntent() {
        return intent;
    }

    public boolean isAvailable() {
        return isAvailable;
    }

    public int getTarget() {
        return target;
    }

    public static class DocumentIntentBuilder {

        private final MediaSource mediaSource;
        private final int requestCode;

        String contentType;
        boolean allowMultiple;

        DocumentIntentBuilder(int requestCode, MediaSource mediaSource) {
            this.mediaSource = mediaSource;
            this.requestCode = requestCode;
            this.contentType = "*/*";
            this.allowMultiple = false;
        }

        public MediaIntent build() {
            return mediaSource.getGalleryIntent(requestCode, contentType, allowMultiple);
        }

        public DocumentIntentBuilder contentType(String contentType) {
            this.contentType = contentType;
            return this;
        }

        public DocumentIntentBuilder allowMultiple(boolean allowMultiple) {
            this.allowMultiple = allowMultiple;
            return this;
        }

        public void open(Fragment fragment) {
            this.build().open(fragment);
        }

        public void open(Activity activity) {
            this.build().open(activity);
        }

    }

    public static class CameraIntentBuilder {

        private final MediaSource mediaSource;
        private final IntentRegistry intentRegistry;
        private final int requestCode;

        boolean video;

        CameraIntentBuilder(int requestCode, MediaSource mediaSource,
                                   IntentRegistry intentRegistry) {
            this.requestCode = requestCode;
            this.mediaSource = mediaSource;
            this.intentRegistry = intentRegistry;
            this.video = false;
        }

        public MediaIntent build() {
            final Pair<MediaIntent, MediaResult> cameraIntent = mediaSource.getCameraIntent(requestCode);
            final MediaIntent mediaIntent = cameraIntent.first;
            final MediaResult result = cameraIntent.second;

            if(mediaIntent.isAvailable()){
                intentRegistry.updateRequestCode(requestCode, result);
            }

            return mediaIntent;
        }

        public void open(Fragment fragment) {
            this.build().open(fragment);
        }

        public void open(Activity activity) {
            this.build().open(activity);
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull final Parcel dest, final int flags) {
        dest.writeInt(requestCode);
        dest.writeParcelable(intent, flags);
        dest.writeString(permission);
        dest.writeBooleanArray(new boolean[]{isAvailable});
        dest.writeInt(target);
    }

    public static final Parcelable.Creator<MediaIntent> CREATOR
            = new Parcelable.Creator<MediaIntent>() {
        public MediaIntent createFromParcel(@NonNull Parcel in) {
            return new MediaIntent(in);
        }

        @NonNull
        public MediaIntent[] newArray(int size) {
            return new MediaIntent[size];
        }
    };

    MediaIntent(Parcel in) {
        this.requestCode = in.readInt();
        this.intent = in.readParcelable(MediaIntent.class.getClassLoader());
        this.permission = in.readString();

        boolean[] isAvailable = new boolean[1];
        in.readBooleanArray(isAvailable);
        this.isAvailable = isAvailable[0];

        this.target = in.readInt();
    }
}
