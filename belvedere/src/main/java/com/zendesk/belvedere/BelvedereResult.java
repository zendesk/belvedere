package com.zendesk.belvedere;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

import java.io.File;

/**
 * Model object, used to return results.
 */
public class BelvedereResult implements Parcelable {

    static BelvedereResult empty() {
        return new BelvedereResult(null, null);
    }

    private final File file;
    private final Uri uri;

    public BelvedereResult(@NonNull final File file, @NonNull final Uri uri) {
        this.file = file;
        this.uri = uri;
    }

    /**
     * Get the resolved {@link File}.
     *
     * @return The {@link File}
     */
    public File getFile() {
        return file;
    }

    /**
     * Get the {@link Uri} to the {@link File}.
     * <br>
     * The returned {@link Uri} points to the
     * {@link BelvedereFileProvider} and could be used
     * to open the {@link File} in a 3rd party app or share
     * it with other apps.
     *
     * @return The {@link Uri}
     */
    public Uri getUri() {
        return uri;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull final Parcel dest, final int flags) {
        dest.writeSerializable(file);
        dest.writeParcelable(uri, flags);
    }

    public static final Parcelable.Creator<BelvedereResult> CREATOR
            = new Parcelable.Creator<BelvedereResult>() {
        public BelvedereResult createFromParcel(@NonNull Parcel in) {
            return new BelvedereResult(in);
        }

        @NonNull
        public BelvedereResult[] newArray(int size) {
            return new BelvedereResult[size];
        }
    };

    private BelvedereResult(Parcel in) {
        this.file = (File) in.readSerializable();
        this.uri = in.readParcelable(BelvedereResult.class.getClassLoader());
    }

}
