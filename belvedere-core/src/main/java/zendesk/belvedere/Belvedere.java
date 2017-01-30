package zendesk.belvedere;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.File;
import java.util.List;
import java.util.Locale;

/**
 * Media picker manager.
 */
@SuppressWarnings("unused")
public class Belvedere implements InstanceBuilder {

    private final static String LOG_TAG = "Belvedere";

    @SuppressLint("StaticFieldLeak")
    private static Belvedere instance;

    private final Context context;
    private final Logger log;
    private final boolean debug;
    private final String directoryName;

    private Storage storage;
    private IntentRegistry intentRegistry;
    private MediaSource mediaSource;

    Belvedere(Builder builder) {
        this.context = builder.context;
        this.log = builder.logger;
        this.debug = builder.debug;
        this.directoryName = builder.directoryName;

        this.intentRegistry = new IntentRegistry();
        this.storage = new Storage(directoryName, log);
        this.mediaSource = new MediaSource(context, log, storage, intentRegistry);

        log.d(LOG_TAG, "Belvedere initialized");
    }

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

    @NonNull
    public MediaIntent.CameraIntentBuilder camera() {
        final int requestCode = intentRegistry.reserveSlot();
        return new MediaIntent.CameraIntentBuilder(requestCode, mediaSource, intentRegistry);
    }

    @NonNull
    public MediaIntent.DocumentIntentBuilder document() {
        final int requestCode = intentRegistry.reserveSlot();
        return new MediaIntent.DocumentIntentBuilder(requestCode, mediaSource);
    }

    @NonNull
    public Intent getViewIntent(@NonNull Uri uri, @NonNull String contentType) {
        final Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, contentType);
        grantPermissionsForUri(intent, uri);
        return intent;
    }

    @NonNull
    public Intent getShareIntent(@NonNull Uri uri, @NonNull String contentType) {
        final Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
        shareIntent.setType(contentType);
        grantPermissionsForUri(shareIntent, uri);
        return shareIntent;
    }

    /**
     * Parse data from {@link Activity#onActivityResult(int, int, Intent)}.
     * <p>
     * It's important that the same instance of Belvedere is used, which
     * was used to start the dialog or create the {@link BelvedereIntent}.
     *
     * @param requestCode The requestCode provided by {@link Activity#onActivityResult(int, int, Intent)}
     * @param resultCode  The resultCode provided by {@link Activity#onActivityResult(int, int, Intent)}
     * @param data        The {@link Intent} provided by {@link Activity#onActivityResult(int, int, Intent)}
     * @param callback    {@link Callback} that will deliver a list of {@link MediaResult}
     */
    public void getFilesFromActivityOnResult(int requestCode, int resultCode, Intent data,
                                             @NonNull Callback<List<MediaResult>> callback) {
        mediaSource.getFilesFromActivityOnResult(context, requestCode, resultCode, data, callback);
    }

    /**
     * Returns a {@link File} and {@link Uri} for the given file name. The returned file
     * is located in Belvedere's internal cache and can be accessed through
     * {@link BelvedereFileProvider}.
     * <p>
     * Belvedere doesn't keep track of your files, you have to manage them.
     *
     * @param fileName The file name
     * @return A {@link MediaResult}
     */
    @Nullable
    public MediaResult getFile(@NonNull String fileName) {
        final File file = storage.getTempFileForRequestAttachment(context, fileName);
        log.d(LOG_TAG, String.format(Locale.US, "Get internal File: %s", file));

        final Uri uri;

        if (file != null && (uri = storage.getFileProviderUri(context, file)) != null) {
            return new MediaResult(file, uri);
        }

        return null;
    }

    public void resolveUris(@NonNull List<Uri> uris, @NonNull Callback<List<MediaResult>> callback) {
        if(uris != null && uris.size() > 0) {
            ResolveUriTask.start(context, log, storage, callback, uris);
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
        log.d(LOG_TAG, String.format(Locale.US, "Grant Permission - Intent: %s - Uri: %s", intent, uri));
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
        log.d(LOG_TAG, String.format(Locale.US, "Revoke Permission - Uri: %s", uri));
        int permissions = Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION;
        storage.revokePermissionsFromUri(context, uri, permissions);
    }

    /**
     * Clear the internal Belvedere cache.
     */
    public void clearStorage() {
        log.d(LOG_TAG, "Clear Belvedere cache");
        storage.clearStorage(context);
    }
}