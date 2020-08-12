package zendesk.belvedere;

import android.app.Activity;
import android.content.Intent;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.core.util.Pair;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.List;

/**
 * Model for representing a
 */
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

    /**
     * Fire the intent.
     */
    public void open(Fragment fragment) {
        fragment.startActivityForResult(intent, requestCode);
    }

    /**
     * Fire the intent.
     */
    public void open(Activity activity) {
        activity.startActivityForResult(intent, requestCode);
    }

    /**
     * Permission that has to be granted before calling {@link #open(Activity)}
     */
    public String getPermission() {
        return permission;
    }

    /**
     * Get the raw intent
     */
    public Intent getIntent() {
        return intent;
    }

    public boolean isAvailable() {
        return isAvailable;
    }

    /**
     * Get the target of the intent.
     * <br />
     * Either {@link #TARGET_CAMERA} or {@link #TARGET_DOCUMENT}
     */
    public int getTarget() {
        return target;
    }

    int getRequestCode() {
        return requestCode;
    }

    public static class DocumentIntentBuilder {

        private final MediaSource mediaSource;
        private final int requestCode;

        String contentType;
        List<String> additionalTypes;
        boolean allowMultiple;

        DocumentIntentBuilder(int requestCode, MediaSource mediaSource) {
            this.mediaSource = mediaSource;
            this.requestCode = requestCode;
            this.contentType = "*/*";
            this.additionalTypes = new ArrayList<>();
            this.allowMultiple = false;
        }

        /**
         * Create the {@link MediaIntent}
         */
        public MediaIntent build() {
            return mediaSource.getGalleryIntent(requestCode, contentType, allowMultiple, additionalTypes);
        }

        /**
         * Restrict the selection to a content type. Only one of the following should be called as
         * they are mutually exclusive:
         *
         * <li>{@link DocumentIntentBuilder#contentType(String)}</li>
         * <li>{@link DocumentIntentBuilder#contentTypes(List)}</li>
         */
        public DocumentIntentBuilder contentType(String contentType) {
            this.contentType = contentType;
            this.additionalTypes = new ArrayList<>();
            return this;
        }

        /**
         * Restrict the selection to the specified content types. This can be used when allowing the
         * selection of files from a disjoint set (e.g. "image&#47;*" and "text&#47;*"), in which
         * case the content type is set to "*&#47;*".
         * Only one of the following should be called as they are mutually exclusive:
         *
         * <li>{@link DocumentIntentBuilder#contentType(String)}</li>
         * <li>{@link DocumentIntentBuilder#contentTypes(List)}</li>
         *
         * @param contentTypes the allowed content types
         */
        public DocumentIntentBuilder contentTypes(List<String> contentTypes) {
            this.contentType = "*/*";
            this.additionalTypes = new ArrayList<>();
            this.additionalTypes.addAll(contentTypes);
            return this;
        }

        /**
         * Allow to user to select multiple files.
         */
        public DocumentIntentBuilder allowMultiple(boolean allowMultiple) {
            this.allowMultiple = allowMultiple;
            return this;
        }

        /**
         * Fire the intent.
         */
        public void open(Fragment fragment) {
            this.build().open(fragment);
        }

        /**
         * Fire the intent.
         */
        public void open(Activity activity) {
            this.build().open(activity);
        }

    }

    public static class CameraIntentBuilder {

        private final MediaSource mediaSource;
        private final IntentRegistry intentRegistry;
        private final int requestCode;

        private boolean video;

        CameraIntentBuilder(int requestCode, MediaSource mediaSource, IntentRegistry intentRegistry) {
            this.requestCode = requestCode;
            this.mediaSource = mediaSource;
            this.intentRegistry = intentRegistry;
            this.video = false;
        }

        /**
         * Create the {@link MediaIntent}
         */
        public MediaIntent build() {
            final Pair<MediaIntent, MediaResult> cameraIntent = mediaSource.getCameraIntent(requestCode);
            final MediaIntent mediaIntent = cameraIntent.first;
            final MediaResult result = cameraIntent.second;

            if (mediaIntent.isAvailable()) {
                intentRegistry.updateRequestCode(requestCode, result);
            }

            return mediaIntent;
        }

        /**
         * Fire the intent.
         */
        public void open(Fragment fragment) {
            this.build().open(fragment);
        }

        /**
         * Fire the intent.
         */
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
