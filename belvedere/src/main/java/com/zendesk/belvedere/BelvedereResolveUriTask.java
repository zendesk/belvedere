package com.zendesk.belvedere;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * AsyncTask for resolving {@link Uri} to {@link File}
 * <p>
 * We try to open an {@link InputStream} from an {@link Uri}
 * by calling {@link ContentResolver#openInputStream(Uri)}.
 * If an {@link InputStream} was successfully opened, the file will
 * be copied into the private cache of the app.
 * </p>
 */
class BelvedereResolveUriTask extends AsyncTask<Uri, Void, List<BelvedereResult>> {

    private final static String LOG_TAG = "BelvedereResolveUriTask";

    final BelvedereCallback<List<BelvedereResult>> callback;
    final Context context;
    final Logger log;
    final Storage storage;

    BelvedereResolveUriTask(
            @NonNull Context context,
            @NonNull Logger logger,
            @NonNull Storage storage,
            @Nullable BelvedereCallback<List<BelvedereResult>> callback) {
        this.context = context;
        this.log = logger;
        this.storage = storage;
        this.callback = callback;
    }

    @Override
    protected List<BelvedereResult> doInBackground(@NonNull Uri... uris) {
        final List<BelvedereResult> success = new ArrayList<>();

        for (Uri uri : uris) {

            InputStream inputStream = null;
            FileOutputStream fileOutputStream = null;

            try {
                inputStream = context.getContentResolver().openInputStream(uri);
                final File file = storage.getTempFileForGalleryImage(context, uri);

                if (inputStream != null && file != null) {
                    log.d(LOG_TAG, String.format(Locale.US, "Copying media file into private cache - Uri: %s - Dest: %s", uri, file));

                    fileOutputStream = new FileOutputStream(file);

                    final byte[] buf = new byte[1024];
                    int len;
                    while ((len = inputStream.read(buf)) > 0) {
                        fileOutputStream.write(buf, 0, len);
                    }

                    success.add(new BelvedereResult(file, storage.getFileProviderUri(context, file)));

                } else {
                    log.w(
                            LOG_TAG,
                            String.format(
                                    Locale.US,
                                    "Unable to resolve uri. InputStream null = %s, File null = %s",
                                    (inputStream == null), (file == null)
                            )
                    );
                }


            } catch (FileNotFoundException e) {
                log.e(LOG_TAG, String.format(Locale.US, "File not found error copying file, uri: %s", uri), e);

            } catch (IOException e) {
                log.e(LOG_TAG, String.format(Locale.US, "IO Error copying file, uri: %s", uri), e);

            } finally {
                try {
                    if (inputStream != null) {
                        inputStream.close();
                    }
                } catch (IOException e) {
                    log.e(LOG_TAG, "Error closing InputStream", e);
                }
                try {
                    if (fileOutputStream != null) {
                        fileOutputStream.close();
                    }
                } catch (IOException e) {
                    log.e(LOG_TAG, "Error closing FileOutputStream", e);
                }
            }
        }
        return success;
    }

    @Override
    protected void onPostExecute(@NonNull List<BelvedereResult> resolvedUris) {
        super.onPostExecute(resolvedUris);
        if (callback != null) {
            callback.internalSuccess(resolvedUris);
        }
    }
}