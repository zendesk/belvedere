package com.zendesk.belvedere;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.app.Activity;

import java.io.File;
import java.util.List;
import java.util.Locale;


/**
 * Media picker manager.
 */
@SuppressWarnings("unused")
public class Belvedere {

    private final static String LOG_TAG = "Belvedere";

    private final Context context;
    private final BelvedereImagePicker imagePicker;
    private final BelvedereStorage belvedereStorage;
    private final BelvedereLogger log;

    Belvedere(Context context, BelvedereConfig belvedereConfig) {
        this.context = context;
        this.belvedereStorage = new BelvedereStorage(belvedereConfig);
        this.imagePicker = new BelvedereImagePicker(belvedereConfig, belvedereStorage);
        this.log = belvedereConfig.getBelvedereLogger();

        log.d(LOG_TAG, "Belvedere initialized");
    }

    /**
     * Initialize Belvedere with a {@link Context} and get a instance
     * of {@link BelvedereConfig.Builder}
     *
     * @param context A valid {@link Context}
     * @return A {@link BelvedereConfig.Builder}
     * @throws IllegalArgumentException if provided {@link Context} is invalid.
     */
    @NonNull
    public static BelvedereConfig.Builder from(@NonNull Context context) {
        if (context != null && context.getApplicationContext() != null) {
            return new BelvedereConfig.Builder(context.getApplicationContext());
        }

        throw new IllegalArgumentException("Invalid context provided");
    }

    /**
     * Get a list of {@link BelvedereIntent} which can be used to build
     * e.g. a dialog.
     *
     * @return A {@link List} of {@link BelvedereIntent}
     */
    @NonNull
    public List<BelvedereIntent> getBelvedereIntents() {
        return imagePicker.getBelvedereIntents(context);
    }

    /**
     * Show Belvedere's own media picker dialog ({@link BelvedereDialog}).
     *
     * @param fragmentManager A {@link FragmentManager}
     */
    public void showDialog(@NonNull FragmentManager fragmentManager) {
        final List<BelvedereIntent> intents = getBelvedereIntents();
        BelvedereDialog.showDialog(fragmentManager, intents);
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
        imagePicker.getFilesFromActivityOnResult(context, requestCode, resultCode, data, callback);
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
    public BelvedereResult getFileRepresentation(@NonNull String fileName) {
        final File file = belvedereStorage.getTempFileForRequestAttachment(context, fileName);
        log.d(LOG_TAG, String.format(Locale.US, "Get internal File: %s", file));

        final Uri uri;

        if (file != null && (uri = belvedereStorage.getFileProviderUri(context, file)) != null) {
            return new BelvedereResult(file, uri);
        }

        return null;
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
        belvedereStorage.grantPermissionsForUri(context, intent, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
    }

    /**
     * Revoke {@link Intent#FLAG_GRANT_READ_URI_PERMISSION} and {@link Intent#FLAG_GRANT_WRITE_URI_PERMISSION} that were
     * previously granted by {@link #grantPermissionsForUri(Intent, Uri)}.
     *
     * @param uri An {@link Uri}
     */
    public void revokePermissionsForUri(Uri uri) {
        log.d(LOG_TAG, String.format(Locale.US, "Revoke Permission - Uri: %s", uri));
        belvedereStorage.revokePermissionsFromUri(context, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
    }

    /**
     * Check if at least one requested {@link BelvedereSource}
     * is available.
     *
     * @return {@code true} if one or more sources are available,
     *         {@code false} if no source is available.
     */
    public boolean oneOrMoreSourceAvailable(){
        return imagePicker.oneOrMoreSourceAvailable(context);
    }

    /**
     * Check if a specific {@link BelvedereSource} is available.
     *
     * @param source The {@link BelvedereSource} to check.
     * @return {@code true} if the source is available, {@code false} if not.
     */
    public boolean isFunctionalityAvailable(@NonNull final BelvedereSource source) {
        return imagePicker.isFunctionalityAvailable(source, context);
    }

    /**
     * Clear the internal Belvedere cache.
     */
    public void clear() {
        log.d(LOG_TAG, "Clear Belvedere cache");
        belvedereStorage.clearStorage(context);
    }
}