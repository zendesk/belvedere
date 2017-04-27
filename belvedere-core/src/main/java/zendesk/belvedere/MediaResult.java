package zendesk.belvedere;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

import java.io.File;

/**
 * Model object, used to return results.
 */
public class MediaResult implements Parcelable, Comparable<MediaResult> {

    static MediaResult empty() {
        return new MediaResult(null, null, null, null, null, -1L);
    }

    private final File file;
    private final Uri uri;
    private final Uri originalUri;
    private final String name;
    private final String mimeType;
    private final long size;

    public MediaResult(final File file, final Uri uri, final Uri originalUri,
                final String name, final String mimeType, final long size) {
        this.file = file;
        this.uri = uri;
        this.originalUri = originalUri;
        this.mimeType = mimeType;
        this.name = name;
        this.size = size;
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

    public Uri getOriginalUri() {
        return originalUri;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * Get the name of the file.
     */
    public String getName() {
        return name;
    }

    /**
     * Get the mime type of the file
     */
    public String getMimeType() {
        return mimeType;
    }

    /**
     * Gets the file size.
     */
    public long getSize() {
        return size;
    }

    @Override
    public void writeToParcel(final Parcel dest, final int flags) {
        dest.writeSerializable(file);
        dest.writeParcelable(uri, flags);
        dest.writeString(name);
        dest.writeString(mimeType);
        dest.writeParcelable(originalUri, flags);
        dest.writeLong(size);
    }

    public static final Parcelable.Creator<MediaResult> CREATOR
            = new Parcelable.Creator<MediaResult>() {
        public MediaResult createFromParcel(Parcel in) {
            return new MediaResult(in);
        }

        @NonNull
        public MediaResult[] newArray(int size) {
            return new MediaResult[size];
        }
    };

    private MediaResult(Parcel in) {
        this.file = (File) in.readSerializable();
        this.uri = in.readParcelable(MediaResult.class.getClassLoader());
        this.name = in.readString();
        this.mimeType = in.readString();
        this.originalUri = in.readParcelable(MediaResult.class.getClassLoader());
        this.size = in.readLong();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MediaResult that = (MediaResult) o;

        if (size != that.size) return false;
        if (file != null ? !file.equals(that.file) : that.file != null) return false;
        if (uri != null ? !uri.equals(that.uri) : that.uri != null) return false;
        if (originalUri != null ? !originalUri.equals(that.originalUri) : that.originalUri != null)
            return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        return mimeType != null ? mimeType.equals(that.mimeType) : that.mimeType == null;
    }

    @Override
    public int hashCode() {
        int result = file != null ? file.hashCode() : 0;
        result = 31 * result + (uri != null ? uri.hashCode() : 0);
        result = 31 * result + (originalUri != null ? originalUri.hashCode() : 0);
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (mimeType != null ? mimeType.hashCode() : 0);
        result = 31 * result + (int) (size ^ (size >>> 32));
        return result;
    }

    @Override
    public int compareTo(@NonNull MediaResult o) {
        return originalUri.compareTo(o.getOriginalUri());
    }
}
