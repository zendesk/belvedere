package zendesk.belvedere;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
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

    private static final String LOG_TAG = "ResolveUriTask";

    static void start(Context context, Storage storage,
                      Callback<List<MediaResult>> callback, List<Uri> uriList){
        start(context, storage, callback, uriList, null);
    }

    static void start(Context context, Storage storage,
                      Callback<List<MediaResult>> callback, List<Uri> uriList, String subDirectory){

        final ResolveUriTask resolveUriTask = new ResolveUriTask(context, storage, callback, subDirectory);

        final Uri[] uris = uriList.toArray(new Uri[uriList.size()]);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            resolveUriTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, uris);
        } else {
            resolveUriTask.execute(uris);
        }
    }

    private final WeakReference<Callback<List<MediaResult>>> callback;
    private final Context context;
    private final Storage storage;
    private final String subDirectory;

    private ResolveUriTask(Context context, Storage storage,
                           Callback<List<MediaResult>> callback, String subDirectory) {
        this.context = context;
        this.storage = storage;
        this.subDirectory = subDirectory;
        this.callback = new WeakReference<>(callback);
    }

    @Override
    protected List<MediaResult> doInBackground(Uri... uris) {
        final List<MediaResult> success = new ArrayList<>();

        final byte[] buf = new byte[1_048_576];
        InputStream inputStream = null;
        FileOutputStream fileOutputStream = null;

        for (Uri uri : uris) {
            try {
                inputStream = context.getContentResolver().openInputStream(uri);
                final File file = storage.getFileForUri(context, uri, subDirectory);

                if (inputStream != null && file != null) {
                    L.d(LOG_TAG, String.format(Locale.US, "Copying media file into private cache - Uri: %s - Dest: %s", uri, file));
                    fileOutputStream = new FileOutputStream(file);

                    int len;
                    while ((len = inputStream.read(buf)) > 0) {
                        fileOutputStream.write(buf, 0, len);
                    }

                    final MediaResult r = Storage.getMediaResultForUri(context, uri);
                    success.add(new MediaResult(file, storage.getFileProviderUri(context, file), uri, file.getName(), r.getMimeType(), r.getSize()));

                } else {
                    L.w(
                            LOG_TAG,
                            String.format(
                                    Locale.US,
                                    "Unable to resolve uri. InputStream null = %s, File null = %s",
                                    (inputStream == null), (file == null)
                            )
                    );
                }


            } catch (FileNotFoundException e) {
                L.e(LOG_TAG, String.format(Locale.US, "File not found error copying file, uri: %s", uri), e);

            } catch (IOException e) {
                L.e(LOG_TAG, String.format(Locale.US, "IO Error copying file, uri: %s", uri), e);

            } finally {
                try {
                    if (inputStream != null) {
                        inputStream.close();
                    }
                } catch (IOException e) {
                    L.e(LOG_TAG, "Error closing InputStream", e);
                }
                try {
                    if (fileOutputStream != null) {
                        fileOutputStream.close();
                    }
                } catch (IOException e) {
                    L.e(LOG_TAG, "Error closing FileOutputStream", e);
                }
            }
        }

        return success;
    }

    @Override
    protected void onPostExecute(List<MediaResult> resolvedUris) {
        super.onPostExecute(resolvedUris);
        if (callback.get() != null) {
            callback.get().internalSuccess(resolvedUris);
        } else {
            L.w(LOG_TAG, "Callback null");
        }
    }
}