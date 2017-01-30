package zendesk.belvedere;

import android.content.Context;
import android.os.Build;

import java.util.Arrays;
import java.util.TreeSet;

/**
 * Class that represents the configuration of {@link Belvedere}
 */
public class BelvedereConfig {

    private String directoryName;

    private int galleryRequestCode;
    private int cameraRequestCodeStart;
    private int cameraRequestCodeEnd;

    private boolean allowMultiple;

    private String contentType;

    private BelvedereLogger belvedereLogger;

    private TreeSet<BelvedereSource> sources;

    private String cameraImagePrefix;

    BelvedereConfig(Builder builder) {
        this.directoryName = builder.mDirectory;
        this.galleryRequestCode = builder.mGalleryRequestCode;
        this.cameraRequestCodeStart = builder.mCameraRequestCodeStart;
        this.cameraRequestCodeEnd = builder.mCameraRequestCodeEnd;
        this.allowMultiple = builder.mAllowMultiple;
        this.contentType = builder.mContentType;
        this.belvedereLogger = builder.mBelvedereLogger;
        this.sources = builder.mSources;
        this.cameraImagePrefix = builder.cameraImagePrefix;
    }

    String getDirectoryName() {
        return directoryName;
    }

    int getGalleryRequestCode() {
        return galleryRequestCode;
    }

    int getCameraRequestCodeStart() {
        return cameraRequestCodeStart;
    }

    int getCameraRequestCodeEnd() {
        return cameraRequestCodeEnd;
    }

    boolean allowMultiple() {
        return allowMultiple;
    }

    String getContentType() {
        return contentType;
    }

    BelvedereLogger getBelvedereLogger() {
        return belvedereLogger;
    }

    TreeSet<BelvedereSource> getBelvedereSources() {
        return sources;
    }

    String getCameraImagePrefix() {
        return cameraImagePrefix;
    }

    /**
     * Builder class for creating a {@link Belvedere} instance.
     */
    public static class Builder{

        private Context mContext;
        private String mDirectory = "belvedere-data";

        private int mGalleryRequestCode = 1602;
        private int mCameraRequestCodeStart = 1603;
        private int mCameraRequestCodeEnd = 1653;

        private boolean mAllowMultiple = true;

        private String mContentType = "*/*";
        private BelvedereLogger mBelvedereLogger = new DefaultLogger();
        private boolean mDebugEnabled = false;
        private TreeSet<BelvedereSource> mSources = new TreeSet<>(Arrays.asList(BelvedereSource.Camera, BelvedereSource.Gallery));
        private String cameraImagePrefix = BelvedereStorage.CAMERA_IMG_PREFIX;

        Builder(Context context){
            this.mContext = context;
        }

        /**
         * Specify a custom gallery request code.
         * <p>
         *     Default: 1602.
         * </p>
         *
         * @param requestCode The new request code
         * @return The {@link Builder}
         */
        public Builder withGalleryRequestCode(int requestCode){
            this.mGalleryRequestCode = requestCode;
            return this;
        }

        /**
         * Specify a new range of request codes for the camera.
         * <p>
         *     startRequestCode must be smaller than endRequestCode. There
         *     must be at least 5 request codes that can be used by
         *     Belvedere.
         * </p>
         * <p>
         *     Default: 1603 - 1620
         * </p>
         *
         * @param startRequestCode The start request code
         * @param endRequestCode The end request code
         * @return The {@link Builder}
         * @throws IllegalArgumentException if an invalid range was provided
         */
        public Builder withCameraRequestCode(int startRequestCode, int endRequestCode){
            if((endRequestCode - startRequestCode) < 5){
                throw new IllegalArgumentException(
                        "The following formula must be apply for the given arguments: " +
                        "(endRequestCode - startRequestCode) >= 5"
                );
            }

            this.mCameraRequestCodeStart = startRequestCode;
            this.mCameraRequestCodeEnd = endRequestCode;
            return this;
        }

        /**
         * Allow the user to select multiple media files. Not available
         * in all gallery apps.
         * <p>
         *     Only available on devices running {@link Build.VERSION_CODES#JELLY_BEAN_MR2}
         *     and above.
         * </p>
         * <p>
         *     Default: true
         * </p>
         *
         * @param allowMultiple True allow multiple media files, False if not.
         * @return The {@link Builder}
         */
        public Builder withAllowMultiple(boolean allowMultiple){
            this.mAllowMultiple = allowMultiple;
            return this;
        }

        /**
         * Specify a custom content type for requesting media from the gallery.
         * <p>
         *     For images use: "images/&#42;"
         * </p>
         * <p>
         *     Default: "&#42;/&#42;"
         * </p>
         *
         * @param contentType The content type.
         * @return The {@link Builder}
         */
        public Builder withContentType(String contentType){
            this.mContentType = contentType;
            return this;
        }

        /**
         * Specify a custom {@link BelvedereLogger}.
         * <p>
         *     Default: {@link DefaultLogger}
         * </p>
         *
         * @param belvedereLogger New {@link BelvedereLogger}
         * @return The {@link Builder}
         * @throws IllegalArgumentException if an invalid {@link BelvedereLogger} was provided.
         */
        public Builder withCustomLogger(BelvedereLogger belvedereLogger){
            if(belvedereLogger != null) {
                this.mBelvedereLogger = belvedereLogger;
                return this;
            }

            throw new IllegalArgumentException("Invalid logger provided");
        }

        /**
         * Specify a list if {@link BelvedereSource} that should be used.
         * <p>
         *     At least one source must be provided. Duplicates will be ignored.
         * </p>
         * <p>
         *     Default: {@link BelvedereSource#Camera}, {@link BelvedereSource#Gallery}
         * </p>
         *
         * @param sources A list of {@link BelvedereSource}
         * @return The {@link Builder}
         * @throws IllegalArgumentException if an invalid {@link BelvedereSource} were provided.
         *          At least one source must be provided.
         */
        public Builder withSource(BelvedereSource... sources){
            if(sources == null || sources.length == 0){
                throw new IllegalArgumentException("Please provide at least one source");
            }

            this.mSources = new TreeSet<>(Arrays.asList(sources));
            return this;
        }

        /**
         * Enable/Disable logging.
         * <p>
         *     Default: False
         * </p>
         *
         * @param enabled True logging enabled, False not
         * @return The {@link Builder}
         */
        public Builder withDebug(boolean enabled){
            this.mDebugEnabled = enabled;
            return this;
        }

        /**
         * Specify a string to prefix an image timestamp to make up the camera image file name
         * <p>
         *     Default: "camera_img_"
         * </p>
         *
         * @param prefix The prefix string
         * @return The {@link Builder}
         */
        public Builder withCameraImagePrefix(String prefix) {
            this.cameraImagePrefix = prefix;
            return this;
        }

        /**
         * Build an instance of {@link Belvedere} and
         * let the magic happen.
         *
         * @return {@link Belvedere}
         */
        public Belvedere build(){
            mBelvedereLogger.setLoggable(mDebugEnabled);
            return new Belvedere(mContext, new BelvedereConfig(this));
        }
    }
}
