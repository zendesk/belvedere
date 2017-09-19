package zendesk.belvedere;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;
import android.util.Pair;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.util.concurrent.atomic.AtomicBoolean;

import zendesk.belvedere.ui.R;

public class FixedWidthImageView extends AppCompatImageView implements Target {

    private static final String LOG_TAG = "FixedWidthImageView";

    private int viewWidth = -1;
    private int viewHeight = -1;

    private int rawImageWidth;
    private int rawImageHeight;

    private Uri uri = null;
    private Picasso picasso;

    private final AtomicBoolean imageWaiting = new AtomicBoolean(false);
    private DimensionsCallback dimensionsCallback;

    public FixedWidthImageView(Context context) {
        super(context);
        init();
    }

    public FixedWidthImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public FixedWidthImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    void init() {
        viewHeight = getResources().getDimensionPixelOffset(R.dimen.belvedere_image_stream_image_height);
    }

    public void showImage(final Picasso picasso, final Uri uri, CalculatedDimensions dimensions) {

        if(uri == null || uri.equals(this.uri)) {
            L.d(LOG_TAG, "Image already loaded. " + uri);
            return;
        }

        // cancel running picasso operations
        if(this.picasso != null) {
            this.picasso.cancelRequest((Target) this);
            this.picasso.cancelRequest((ImageView) this);
        }

        this.uri = uri;
        this.picasso = picasso;
        this.rawImageWidth = dimensions.rawImageWidth;
        this.rawImageHeight = dimensions.rawImageHeight;
        this.viewHeight = dimensions.viewHeight;
        this.viewWidth = dimensions.viewWidth;

        startImageLoading(picasso, uri, viewWidth, rawImageWidth, rawImageHeight);
    }

    public void showImage(final Picasso picasso, final Uri uri, long rawImageWidth, long rawImageHeight,
                          DimensionsCallback dimensionsCallback) {

        if(uri == null || uri.equals(this.uri)) {
            L.d(LOG_TAG, "Image already loaded. " + uri);
            return;
        }

        // cancel running picasso operations
        if(this.picasso != null) {
            this.picasso.cancelRequest((Target) this);
            this.picasso.cancelRequest((ImageView) this);
        }

        this.uri = uri;
        this.picasso = picasso;
        this.rawImageWidth = (int) rawImageWidth;
        this.rawImageHeight = (int) rawImageHeight;
        this.dimensionsCallback = dimensionsCallback;

        if(viewWidth > 0) {
            startImageLoading(picasso, uri, viewWidth, this.rawImageWidth, this.rawImageHeight);
        } else {
            imageWaiting.set(true);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int measuredWidth = MeasureSpec.getSize(widthMeasureSpec);

        final int finalHeight = MeasureSpec.makeMeasureSpec(viewHeight, MeasureSpec.EXACTLY);
        final int finalWidth;

        if(viewWidth == -1) {
            // the width is known; save it
            viewWidth = measuredWidth;
        }

        if(viewWidth > 0) {
            // the width is known, use it
            finalWidth = MeasureSpec.makeMeasureSpec(viewWidth, MeasureSpec.EXACTLY);

            // showImage() was called before we knew the view size
            // now it's the time to load the image
            if(imageWaiting.compareAndSet(true, false)) {
                startImageLoading(picasso, uri, viewWidth, rawImageWidth, rawImageHeight);
            }

        } else {
            finalWidth = widthMeasureSpec;
        }

        super.onMeasure(finalWidth, finalHeight);
    }

    private Pair<Integer, Integer> scale(int width, int imageWidth, int imageHeight) {
        final float scaleFactor = (float)width / imageWidth;
        return Pair.create(width, (int)(imageHeight * scaleFactor));
    }

    private void startImageLoading(Picasso picasso, Uri uri, int viewWidth, int rawImageWidth, int rawImageHeight) {
        L.d(LOG_TAG, "Start loading image: " + viewWidth + " " + rawImageWidth + " " + rawImageHeight);
        if(rawImageWidth > 0 && rawImageHeight > 0) {
            final Pair<Integer, Integer> scaledDimensions =
                    scale(viewWidth, rawImageWidth, rawImageHeight);
            loadImage(picasso, scaledDimensions.first, scaledDimensions.second, uri);
        } else {
            picasso.load(uri).into((Target) this);
        }
    }

    private void loadImage(Picasso picasso, int scaledImageWidth, int scaledImageHeight, Uri uri) {
        // the image height is known. update the view
        this.viewHeight = scaledImageHeight;

        // posting requestlayout makes it work better ... no idea why
        post(new Runnable() {
            @Override
            public void run() {
                requestLayout();
            }
        });

        if(dimensionsCallback != null) {
            dimensionsCallback.onImageDimensionsFound(
                    new CalculatedDimensions(rawImageHeight, rawImageWidth, viewHeight, viewWidth));
            dimensionsCallback = null;
        }

        picasso.load(uri)
                .resize(scaledImageWidth, scaledImageHeight)
                .transform(Utils.roundTransformation(getContext(), R.dimen.belvedere_image_stream_item_radius))
                .into((ImageView) this);
    }

    @Override
    public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
        this.rawImageHeight = bitmap.getHeight();
        this.rawImageWidth = bitmap.getWidth();

        Pair<Integer, Integer> scaledDimensions = scale(viewWidth, rawImageWidth, rawImageHeight);
        loadImage(picasso, scaledDimensions.first, scaledDimensions.second, uri);
    }

    @Override
    public void onBitmapFailed(Drawable errorDrawable) {
        // intentionally empty
    }

    @Override
    public void onPrepareLoad(Drawable placeHolderDrawable) {
        // intentionally empty
    }

    interface DimensionsCallback {
        void onImageDimensionsFound(CalculatedDimensions dimensions);
    }

    static class CalculatedDimensions {

        private final int rawImageHeight;
        private final int rawImageWidth;
        private final int viewHeight;
        private final int viewWidth;

        CalculatedDimensions(int rawImageHeight, int rawImageWidth, int viewHeight, int viewWidth) {
            this.rawImageHeight = rawImageHeight;
            this.rawImageWidth = rawImageWidth;
            this.viewHeight = viewHeight;
            this.viewWidth = viewWidth;
        }
    }
}
