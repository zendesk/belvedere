package com.zendesk.belvedere;

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
            if(instance == null) {
                if (context != null && context.getApplicationContext() != null) {
                    return new Builder(context.getApplicationContext()).build();
                } else {
                    throw new IllegalArgumentException("Invalid context provided");
                }
            }
        }

        return instance;
    }

    public static void setSingletonInstance(@NonNull Belvedere belvedere) {
        if (belvedere == null) {
            throw new IllegalArgumentException("Picasso must not be null.");
        }
        synchronized (Belvedere.class) {
            if (instance != null) {
                throw new IllegalStateException("Singleton instance already exists.");
            }
            instance = belvedere;
        }
    }


    public MediaIntent.CameraIntentBuilder camera() {
        final int requestCode = intentRegistry.reserveSlot();
        return new MediaIntent.CameraIntentBuilder(requestCode, mediaSource, intentRegistry);
    }

    public MediaIntent.DocumentIntentBuilder document() {
        final int requestCode = intentRegistry.reserveSlot();
        return new MediaIntent.DocumentIntentBuilder(requestCode, mediaSource);
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
     * @param callback    {@link BelvedereCallback} that will deliver a list of {@link BelvedereResult}
     */
    public void getFilesFromActivityOnResult(int requestCode, int resultCode, Intent data, @NonNull BelvedereCallback<List<BelvedereResult>> callback) {
        mediaSource.getFilesFromActivityOnResult(context, requestCode, resultCode, data, callback); // FIXME
    }

    /**
     * Returns a {@link File} and {@link Uri} for the given file name. The returned file
     * is located in Belvedere's internal cache and can be accessed through
     * {@link BelvedereFileProvider}.
     * <p>
     * Belvedere doesn't keep track of your files, you have to manage them.
     *
     * @param fileName The file name
     * @return A {@link BelvedereResult}
     */
    @Nullable
    public BelvedereResult getFile(@NonNull String fileName) {
        final File file = storage.getTempFileForRequestAttachment(context, fileName);
        log.d(LOG_TAG, String.format(Locale.US, "Get internal File: %s", file));

        final Uri uri;

        if (file != null && (uri = storage.getFileProviderUri(context, file)) != null) {
            return new BelvedereResult(file, uri);
        }

        return null;
    }

    public void resolveUris(List<Uri> uris, BelvedereCallback<List<BelvedereResult>> callback) {
        new BelvedereResolveUriTask(context, log, storage, callback)
                .execute(uris.toArray(new Uri[uris.size()]));
    }

    /**
     * Grant all Apps that are resolvable through the provided {@link Intent} {@link Intent#FLAG_GRANT_READ_URI_PERMISSION} and
     * {@link Intent#FLAG_GRANT_WRITE_URI_PERMISSION}.
     *
     * @param intent An {@link Intent}
     * @param uri    An {@link Uri}
     */
    public void grantPermissionsForUri(Intent intent, Uri uri) {
        log.d(LOG_TAG, String.format(Locale.US, "Grant Permission - Intent: %s - Uri: %s", intent, uri));
        storage.grantPermissionsForUri(context, intent, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
    }

    /**
     * Revoke {@link Intent#FLAG_GRANT_READ_URI_PERMISSION} and {@link Intent#FLAG_GRANT_WRITE_URI_PERMISSION} that were
     * previously granted by {@link #grantPermissionsForUri(Intent, Uri)}.
     *
     * @param uri An {@link Uri}
     */
    public void revokePermissionsForUri(Uri uri) {
        log.d(LOG_TAG, String.format(Locale.US, "Revoke Permission - Uri: %s", uri));
        storage.revokePermissionsFromUri(context, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
    }
    /**
     * Clear the internal Belvedere cache.
     */
    public void clearStorage() {
        log.d(LOG_TAG, "Clear Belvedere cache");
        storage.clearStorage(context);
    }
}