package zendesk.belvedere;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;

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
class ResolveUriTask extends AsyncTask<Uri, Void, List<MediaResult>> {

    private final static String LOG_TAG = "BelvedereResolveUriTask";

    private final Callback<List<MediaResult>> callback;
    private final Context context;
    private final Logger log;
    private final Storage storage;

    ResolveUriTask(Context context, Logger logger, Storage storage,
                   Callback<List<MediaResult>> callback) {
        this.context = context;
        this.log = logger;
        this.storage = storage;
        this.callback = callback;
    }

    @Override
    protected List<MediaResult> doInBackground(Uri... uris) {
        final List<MediaResult> success = new ArrayList<>();

        final byte[] buf = new byte[1024 * 1024];
        InputStream inputStream = null;
        FileOutputStream fileOutputStream = null;

        for (Uri uri : uris) {
            try {
                inputStream = context.getContentResolver().openInputStream(uri);
                final File file = storage.getTempFileForGalleryImage(context, uri);

                if (inputStream != null && file != null) {
                    log.d(LOG_TAG, String.format(Locale.US, "Copying media file into private cache - Uri: %s - Dest: %s", uri, file));
                    fileOutputStream = new FileOutputStream(file);

                    int len;
                    while ((len = inputStream.read(buf)) > 0) {
                        fileOutputStream.write(buf, 0, len);
                    }

                    success.add(new MediaResult(file, storage.getFileProviderUri(context, file)));

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
    protected void onPostExecute(List<MediaResult> resolvedUris) {
        super.onPostExecute(resolvedUris);
        if (callback != null) {
            callback.internalSuccess(resolvedUris);
        }
    }
}