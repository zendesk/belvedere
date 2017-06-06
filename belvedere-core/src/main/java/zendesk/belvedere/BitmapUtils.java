package zendesk.belvedere;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.annotation.NonNull;
import android.util.Pair;

import java.io.File;

/**
 * Utility methods for {@link Bitmap}
 */
@SuppressWarnings("WeakerAccess")
public class BitmapUtils {

    /**
     * Gets the dimension of an image file.
     *
     * @param file The image file
     * @return The width and the height of an image file.
     */
    @SuppressWarnings("WeakerAccess")
    public static Pair<Integer, Integer> getImageDimensions(@NonNull File file) {
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(file.getAbsolutePath(), options); // I/O on the main thread but only takes ~0.5ms
        return Pair.create(options.outWidth, options.outHeight);
    }
}
