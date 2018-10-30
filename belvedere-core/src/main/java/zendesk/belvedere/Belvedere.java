package zendesk.belvedere;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.text.TextUtils;
import android.util.Pair;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Media picker manager.
 */
@SuppressWarnings("unused")
public class Belvedere {

    final static String LOG_TAG = "Belvedere";
    private static final String MIME_TYPE_IMAGE = "image";

    @SuppressLint("StaticFieldLeak")
    private static Belvedere instance;

    private final Context context;

    private Storage storage;
    private IntentRegistry intentRegistry;
    private MediaSource mediaSource;

    Belvedere(Builder builder) {
        this.context = builder.context;

        builder.logger.setLoggable(builder.debug);
        L.setLogger(builder.logger);

        this.intentRegistry = new IntentRegistry();
        this.storage = new Storage();
        this.mediaSource = new MediaSource(context, storage, intentRegistry);

        L.d(LOG_TAG, "Belvedere initialized");
    }

    /**
     * Get the global {@link Belvedere} instance.
     *
     */
    @NonNull
    public static Belvedere from(@NonNull Context context) {
        synchronized (Belvedere.class) {
            if (instance == null) {
                if (context != null && context.getApplicationContext() != null) {
                    instance = new Builder(context.getApplicationContext()).build();
                } else {
                    throw new IllegalArgumentException("Invalid context provided");
                }
            }
        }

        return instance;
    }

    /**
     * Set the global {@link Belvedere} instance.
     * <br />
     * This must be called before calling {@link Belvedere#from(Context)}
     *
     * @param belvedere an instance created through {@link Belvedere.Builder}
     */
    public static void setSingletonInstance(@NonNull Belvedere belvedere) {
        if (belvedere == null) {
            throw new IllegalArgumentException("Belvedere must not be null.");
        }
        synchronized (Belvedere.class) {
            if (instance != null) {
                throw new IllegalStateException("Singleton instance already exists.");
            }
            instance = belvedere;
        }
    }

    /**
     * Request an image from a camera app.
     *
     * <pre>
     * Belvedere.from(context)
     *   .camera()
     *   .open(activity);
     * </pre>
     */
    @NonNull
    public MediaIntent.CameraIntentBuilder camera() {
        final int requestCode = intentRegistry.reserveSlot();
        return new MediaIntent.CameraIntentBuilder(requestCode, mediaSource, intentRegistry);
    }

    /**
     * Request media from an external app.
     *
     * <pre>
     * Belvedere.from(context)
     *   .document()
     *   ...
     *   .open(activity);
     * </pre>
     */
    @NonNull
    public MediaIntent.DocumentIntentBuilder document() {
        final int requestCode = intentRegistry.reserveSlot();
        return new MediaIntent.DocumentIntentBuilder(requestCode, mediaSource);
    }

    /**
     * Create an {@link Intent} for viewing an {@link Uri}.
     *
     * @param uri {@link Uri} pointing to a file
     * @param contentType (optional) the content type of the {@link Uri}
     */
    @NonNull
    public Intent getViewIntent(@NonNull Uri uri, @Nullable String contentType) {
        final Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        if (!TextUtils.isEmpty(contentType)) {
            intent.setDataAndType(uri, contentType);
        }
        grantPermissionsForUri(intent, uri);
        return intent;
    }

    /**
     * Create an {@link Intent} for sharing an {@link Uri}.
     *
     * @param uri {@link Uri} pointing to a file
     * @param contentType (optional) the content type of the {@link Uri}
     */
    @NonNull
    public Intent getShareIntent(@NonNull Uri uri, @NonNull String contentType) {
        final Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
        if (!TextUtils.isEmpty(contentType)) {
            shareIntent.setType(contentType);
        }
        grantPermissionsForUri(shareIntent, uri);
        return shareIntent;
    }


    /**
     * Parse data from {@link Activity#onActivityResult(int, int, Intent)}.
     * <p>
     * It's important that the same instance of Belvedere is used, which
     * was used to start the dialog or create the {@link MediaIntent}.
     *
     * @param requestCode The requestCode provided by {@link Activity#onActivityResult(int, int, Intent)}
     * @param resultCode  The resultCode provided by {@link Activity#onActivityResult(int, int, Intent)}
     * @param data        The {@link Intent} provided by {@link Activity#onActivityResult(int, int, Intent)}
     * @param callback    {@link Callback} that will deliver a list of {@link MediaResult}
     */
    public void getFilesFromActivityOnResult(int requestCode, int resultCode, Intent data,
                                             @NonNull Callback<List<MediaResult>> callback) {
        getFilesFromActivityOnResult(requestCode, resultCode, data, callback, true);
    }

    /**
     * Parse data from {@link Activity#onActivityResult(int, int, Intent)}.
     *
     * @param requestCode The requestCode provided by {@link Activity#onActivityResult(int, int, Intent)}
     * @param resultCode The resultCode provided by {@link Activity#onActivityResult(int, int, Intent)}
     * @param data The {@link Intent} provided by {@link Activity#onActivityResult(int, int, Intent)}
     * @param callback {@link Callback} that will deliver a list of {@link MediaResult}
     * @param resolveFiles Set to {@code true} if belvedere should resolve selected files
     */
    public void getFilesFromActivityOnResult(int requestCode, int resultCode, Intent data,
                                             @NonNull Callback<List<MediaResult>> callback,
                                             boolean resolveFiles) {
        mediaSource.getFilesFromActivityOnResult(context, requestCode, resultCode, data, callback, resolveFiles);
    }

    /**
     * Returns a {@link File} and {@link Uri} for the given file name. The returned file
     * is located in Belvedere's internal cache and can be accessed through
     * {@link BelvedereFileProvider}.
     * <p>
     * Belvedere doesn't keep track of your files, you have to manage them.
     *
     * @param dir Directory name of the file or {@code null} if not needed.
     * @param fileName The file name
     * @return A {@link MediaResult}
     */
    @Nullable
    public MediaResult getFile(@NonNull String dir, @NonNull String fileName) {
        final File file = storage.getFile(context, dir, fileName);
        L.d(LOG_TAG, String.format(Locale.US, "Get internal File: %s", file));

        final Uri uri;

        if (file != null && (uri = storage.getFileProviderUri(context, file)) != null) {
            final MediaResult r = Storage.getMediaResultForUri(context, uri);

            final long width, height;
            if (r.getMimeType().contains(MIME_TYPE_IMAGE)) {
                final Pair<Integer, Integer> imageDimensions = BitmapUtils.getImageDimensions(file);
                width = imageDimensions.first;
                height = imageDimensions.second;
            } else {
                width = MediaResult.UNKNOWN_VALUE;
                height = MediaResult.UNKNOWN_VALUE;
            }
            return new MediaResult(file, uri, uri, fileName, r.getMimeType(), r.getSize(), width, height);
        }

        return null;
    }

    /**
     * Copy the list of provided {@link Uri} into the internal cache.
     *
     * @param uris The list of {@link Uri} to resolve
     * @param directory Name of directory for storing them
     * @param callback {@link Callback} that will deliver a list of {@link MediaResult}
     */
    public void resolveUris(@NonNull List<Uri> uris, @NonNull String directory, @NonNull Callback<List<MediaResult>> callback) {
        if(uris != null && uris.size() > 0) {
            ResolveUriTask.start(context, storage,  callback, uris, directory);
        } else {
            callback.internalSuccess(new ArrayList<MediaResult>(0));
        }
    }

    /**
     * Grant all Apps that are resolvable through the provided {@link Intent} {@link Intent#FLAG_GRANT_READ_URI_PERMISSION} and
     * {@link Intent#FLAG_GRANT_WRITE_URI_PERMISSION}.
     *
     * @param intent An {@link Intent}
     * @param uri    An {@link Uri}
     */
    public void grantPermissionsForUri(@NonNull Intent intent, @NonNull Uri uri) {
        L.d(LOG_TAG, String.format(Locale.US, "Grant Permission - Intent: %s - Uri: %s", intent, uri));
        int permissions = Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION;
        storage.grantPermissionsForUri(context, intent, uri, permissions);
    }

    /**
     * Revoke {@link Intent#FLAG_GRANT_READ_URI_PERMISSION} and {@link Intent#FLAG_GRANT_WRITE_URI_PERMISSION} that were
     * previously granted by {@link #grantPermissionsForUri(Intent, Uri)}.
     *
     * @param uri An {@link Uri}
     */
    public void revokePermissionsForUri(@NonNull Uri uri) {
        L.d(LOG_TAG, String.format(Locale.US, "Revoke Permission - Uri: %s", uri));
        int permissions = Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION;
        storage.revokePermissionsFromUri(context, uri, permissions);
    }

    /**
     * Clear the internal Belvedere cache.
     */
    public void clearStorage() {
        L.d(LOG_TAG, "Clear Belvedere cache");
        storage.clearStorage(context);
    }

    /**
     * Builder for creating a customize {@link Belvedere} instance.
     * <br /><br />
     * Example:
     * <pre>
     * Belvedere belvedere = new Belvedere.Builder(this)
     *   .debug(true)
     *   .build();
     * Belvedere.setSingletonInstance(belvedere);
     * </pre>
     */
    public static class Builder {

        Context context;
        L.Logger logger;
        boolean debug;

        public Builder(Context context) {
            this.context = context;
            this.logger = new L.DefaultLogger();
            this.debug = false;
        }

        /**
         * Provide a custom implementation of {@link L.Logger}
         */
        public Builder logger(L.Logger logger) {
            this.logger = logger;
            return this;
        }

        /**
         * Enable/disable logging.
         */
        public Builder debug(boolean debug) {
            this.debug = debug;
            return this;
        }

        /**
         * Create a custom instance of {@link Belvedere}
         */
        public Belvedere build() {
            return new Belvedere(this);
        }
    }
}