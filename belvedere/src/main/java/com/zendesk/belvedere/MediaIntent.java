package com.zendesk.belvedere;

import android.app.Activity;
import android.content.Intent;
import android.support.v4.app.Fragment;
import android.support.v4.util.Pair;

public class MediaIntent {

    static MediaIntent notAvailable() {
        return new MediaIntent(-1, null, null, false);
    }

    private final boolean isAvailable;
    private final int requestCode;
    private final Intent intent;
    private final String permission;

    MediaIntent(int requestCode, Intent intent, String permission, boolean isAvailable) {
        this.requestCode = requestCode;
        this.intent = intent;
        this.permission = permission;
        this.isAvailable = isAvailable;
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
                return new MediaIntent(requestCode, mediaIntent.getIntent(), mediaIntent.getPermission(), true);
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
                return new MediaIntent(requestCode, intent.getIntent(), intent.getPermission(), true);
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
