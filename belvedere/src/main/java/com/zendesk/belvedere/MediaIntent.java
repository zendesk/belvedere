package com.zendesk.belvedere;

import android.app.Activity;
import android.content.Intent;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.util.Pair;

public class MediaIntent implements Parcelable {

    public static int TARGET_DOCUMENT = 1;
    public static int TARGET_CAMERA = 2;

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

    MediaIntent(Parcel in) {
        this.requestCode = in.readInt();
        this.intent = in.readParcelable(BelvedereIntent.class.getClassLoader());
        this.permission = in.readString();

        boolean[] isAvailable = new boolean[1];
        in.readBooleanArray(isAvailable);
        this.isAvailable = isAvailable[0];

        this.target = in.readInt();
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
            final BelvedereIntent mediaIntent
                    = mediaSource.getGalleryIntent(requestCode, contentType, allowMultiple);
            if(mediaIntent != null){
                return new MediaIntent(requestCode, mediaIntent.getIntent(), mediaIntent.getPermission(), true, TARGET_DOCUMENT);
            } else {
                return MediaIntent.notAvailable();
            }
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
            final Pair<BelvedereIntent, BelvedereResult> cameraIntent =
                    mediaSource.getCameraIntent(requestCode);
            if(cameraIntent != null){
                final BelvedereIntent intent = cameraIntent.first;
                final BelvedereResult result = cameraIntent.second;
                intentRegistry.updateRequestCode(requestCode, result);
                return new MediaIntent(requestCode, intent.getIntent(), intent.getPermission(), true, TARGET_CAMERA);
            } else {
                return MediaIntent.notAvailable();
            }
        }

        public void open(Fragment fragment) {
            this.build().open(fragment);
        }

        public void open(Activity activity) {
            this.build().open(activity);
        }
    }

}
