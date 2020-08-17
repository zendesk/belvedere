package zendesk.belvedere;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;

import androidx.annotation.Nullable;

import java.util.Locale;

import static android.content.ContentResolver.QUERY_SORT_DIRECTION_DESCENDING;

public class ImageStreamCursorProvider {

    final static String[] PROJECTION = new String[]{
            MediaStore.Images.ImageColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.WIDTH,
            MediaStore.MediaColumns.HEIGHT
    };

    private final Context context;
    private final int currentApiLevel;

    /**
     * Constructs an ImageStreamCursorProvider
     *
     * @param context A valid context
     * @param currentApiLevel The API level of the device running this software
     */
    ImageStreamCursorProvider(Context context, int currentApiLevel) {
        this.context = context;
        this.currentApiLevel = currentApiLevel;
    }

    /**
     * Gets a cursor containing the maximum number of images to request. Can return null.
     *
     * @param count The maximum number of images to request
     * @return A Cursor containing the images, or null
     */
    @SuppressLint("NewApi")
    @Nullable Cursor getCursor(int count) {
        if (context == null) {
            return null;
        }

        final String orderColumn = getOrderColumn();

        Cursor cursor;

        if (currentApiLevel >= Build.VERSION_CODES.O) {
            Bundle queryArgs = new Bundle();

            queryArgs.putInt(ContentResolver.QUERY_ARG_LIMIT, count);
            queryArgs.putStringArray(ContentResolver.QUERY_ARG_SORT_COLUMNS, new String[]{orderColumn});
            queryArgs.putInt(ContentResolver.QUERY_ARG_SORT_DIRECTION, QUERY_SORT_DIRECTION_DESCENDING);

            cursor = context.getContentResolver().query(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    PROJECTION,
                    queryArgs,
                    null);
        } else {
            final String order =
                    String.format(Locale.US, "%s DESC LIMIT %s", orderColumn, count);

            cursor = context.getContentResolver().query(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    PROJECTION,
                    null,
                    null,
                    order);
        }

        return cursor;
    }

    @SuppressLint("InlinedApi")
    String getOrderColumn() {
        return currentApiLevel >= Build.VERSION_CODES.Q
                ? MediaStore.Images.ImageColumns.DATE_TAKEN
                : MediaStore.Images.ImageColumns.DATE_MODIFIED;
    }
}
