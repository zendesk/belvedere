package zendesk.belvedere;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.webkit.MimeTypeMap;

import java.util.ArrayList;
import java.util.List;

class ImageStreamService {

    private final Context context;

    ImageStreamService(Context context) {
        this.context = context.getApplicationContext();
    }

    List<MediaResult> queryRecentImages(int count) {
        final List<MediaResult> mediaResults = new ArrayList<>();

        final Cursor cursor = new ImageStreamCursorProvider(context, Build.VERSION.SDK_INT)
                .getCursor(count);

        try {
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    final Uri uri = MediaStore.Files.getContentUri("external",
                            cursor.getLong(cursor.getColumnIndex(MediaStore.Images.ImageColumns._ID)));

                    final long size = cursor.getLong(cursor.getColumnIndex(MediaStore.MediaColumns.SIZE));
                    final long width = cursor.getLong(cursor.getColumnIndex(MediaStore.MediaColumns.WIDTH));
                    final long height = cursor.getLong(cursor.getColumnIndex(MediaStore.MediaColumns.HEIGHT));
                    final String name = cursor.getString(cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME));

                    String mimeType = "image/jpeg";
                    if (!TextUtils.isEmpty(name)) {
                        final int index = name.lastIndexOf(".");
                        if (index != -1) {
                            mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(name.substring(index + 1));
                        }
                    }

                    mediaResults.add(new MediaResult(null, uri, uri, name, mimeType, size, width, height));
                }
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return mediaResults;
    }

    boolean isAppAvailable(String packageName) {
        return Utils.isAppAvailable(packageName, context);
    }
}
