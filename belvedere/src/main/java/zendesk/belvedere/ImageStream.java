package zendesk.belvedere;


import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import zendesk.belvedere.ui.R;

/**
 * APIs for interacting with the ImageStream
 */
public class ImageStream extends Fragment {

    private WeakReference<KeyboardHelper> keyboardHelper = new WeakReference<>(null);

    private List<WeakReference<Listener>> imageStreamListener = new ArrayList<>();
    private List<WeakReference<ScrollListener>> imageStreamScrollListener = new ArrayList<>();

    private ImageStreamUi imageStreamPopup = null;
    private BelvedereUi.UiConfig uiConfig = null;
    private boolean wasOpen = false;

    private PermissionManager permissionManager;

    private Callback<List<MediaResult>> callback;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        permissionManager = new PermissionManager(getContext());
    }

    @Override
    public void onPause() {
        super.onPause();
        if(imageStreamPopup != null) {
            imageStreamPopup.dismiss();
            wasOpen = true;
        } else {
            wasOpen = false;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        boolean handled = permissionManager.onRequestPermissionsResult(this, requestCode, permissions, grantResults);
        if(!handled) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    public void onActivityResult(int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        callback = new Callback<List<MediaResult>>() {
            @Override
            public void success(List<MediaResult> result) {
                List<MediaResult> filteredMediaResult = new ArrayList<>(result.size());
                for(MediaResult m : result) {
                    if(m.getSize() <= uiConfig.getMaxFileSize() || uiConfig.getMaxFileSize() == -1L) {
                        filteredMediaResult.add(m);
                    }
                }

                if(filteredMediaResult.size() != result.size()) {
                    Toast.makeText(getContext(), R.string.belvedere_image_stream_file_too_large, Toast.LENGTH_SHORT).show();
                }

                notifyImageSelected(filteredMediaResult);
            }
        };

        Belvedere.from(this.getContext()).getFilesFromActivityOnResult(requestCode, resultCode, data, callback, false);
    }

    void setKeyboardHelper(KeyboardHelper keyboardHelper) {
        this.keyboardHelper = new WeakReference<>(keyboardHelper);
    }

    void setImageStreamUi(ImageStreamUi imageStreamPopup, BelvedereUi.UiConfig uiConfig) {
        this.imageStreamPopup = imageStreamPopup;
        if(uiConfig != null) {
            this.uiConfig = uiConfig;
        }
    }

    void notifyScrollListener(int height, int scrollArea, float scrollPosition) {
        for(WeakReference<ScrollListener> ref : imageStreamScrollListener) {
            final ScrollListener scrollListener = ref.get();
            if(scrollListener != null) {
                scrollListener.onScroll(height, scrollArea, scrollPosition);
            }
        }
    }

    void notifyImageSelected(List<MediaResult> mediaResults) {
        for(WeakReference<Listener> ref : imageStreamListener) {
            final Listener listener = ref.get();
            if(listener != null) {
                listener.onMediaSelected(mediaResults);
            }
        }
    }

    void notifyImageDeselected(List<MediaResult> mediaResults) {
        for(WeakReference<Listener> ref : imageStreamListener) {
            final Listener listener = ref.get();
            if(listener != null) {
                listener.onMediaDeselected(mediaResults);
            }
        }
    }

    void notifyDismissed() {
        callback = null; // Prevent memory leak of Callback

        for(WeakReference<Listener> ref : imageStreamListener) {
            final Listener listener = ref.get();
            if(listener != null) {
                listener.onDismissed();
            }
        }
    }

    void notifyVisible() {
        for(WeakReference<Listener> ref : imageStreamListener) {
            final Listener listener = ref.get();
            if(listener != null) {
                listener.onVisible();
            }
        }
    }

    void handlePermissions(final List<MediaIntent> mediaIntents, final PermissionManager.PermissionCallback permissionCallback) {
        permissionManager.handlePermissions(this, mediaIntents, permissionCallback);
    }

    /**
     * Gets the currently install {@link KeyboardHelper}
     */
    public KeyboardHelper getKeyboardHelper() {
        return keyboardHelper.get();
    }

    /**
     * Add a {@link Listener} for getting notified when the users selects/deselects an attachment
     * or the ImageStream gets visible or dismissed.
     */
    public void addListener(Listener listener) {
        imageStreamListener.add(new WeakReference<>(listener));
    }

    /**
     * Add a {@link ScrollListener} to get informed when the ImageStream gets dragged by the user.
     */
    public void addScrollListener(ScrollListener listener) {
        imageStreamScrollListener.add(new WeakReference<>(listener));
    }

    /**
     * Hide the ImageStream if visible
     */
    public void dismiss() {
        if(isAttachmentsPopupVisible()) {
            imageStreamPopup.dismiss();
        }
    }

    /**
     * Check if the ImageStream was visible before the last configuration change.
     *
     * @return {@code true} if it was visible, {@code false} if not
     */
    public boolean wasOpen() {
        return wasOpen;
    }

    /**
     * Check if the ImageStream is currently visible.
     */
    public boolean isAttachmentsPopupVisible() {
        return imageStreamPopup != null;
    }

    /**
     * Listener for observing the user interaction with the ImageStream
     */
    public interface Listener {

        /**
         * The user dismissed the ImageStream.
         */
        void onDismissed();

        /**
         * The ImageStream became visible to the user.
         */
        void onVisible();

        /**
         * The user selected one or multiple attachments.
         */
        void onMediaSelected(List<MediaResult> mediaResults);

        /**
         * The user deselected one or multiple attachments.
         */
        void onMediaDeselected(List<MediaResult> mediaResults);
    }

    /**
     * Informs about the scroll position of the ImageStream BottomSheet.
     */
    public interface ScrollListener {

        /**
         * Called if the ImageStream gets dragged by the user.
         *
         * @param height
         * @param scrollArea
         * @param scrollPosition
         */
        void onScroll(int height, int scrollArea, float scrollPosition);
    }
}