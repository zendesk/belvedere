package zendesk.belvedere;


import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ImageStream extends Fragment {

    private static final int PERMISSION_REQUEST_CODE = 9842;

    private PermissionStorage preferences;
    private InternalPermissionCallback permissionListener = null;

    private WeakReference<KeyboardHelper> keyboardHelper = new WeakReference<>(null);

    private List<WeakReference<Listener>> imageStreamListener = new ArrayList<>();
    private List<WeakReference<ScrollListener>> imageStreamScrollListener = new ArrayList<>();

    private ImageStreamUi imageStreamPopup = null;
    private BelvedereUi.UiConfig uiConfig = null;
    private boolean wasOpen = false;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferences = new PermissionStorage(getContext());
        setRetainInstance(true);
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
        if (requestCode == PERMISSION_REQUEST_CODE) {
            final Map<String, Boolean> permissionResult = new HashMap<>();
            final List<String> dontAskAgain = new ArrayList<>();

            for(int i = 0, c = permissions.length; i < c; i++) {
                if(grantResults[i] == PackageManager.PERMISSION_GRANTED){
                    permissionResult.put(permissions[i], true);

                } else if(grantResults[i] == PackageManager.PERMISSION_DENIED) {
                    permissionResult.put(permissions[i], false);

                    boolean showRationale = shouldShowRequestPermissionRationale(permissions[i]);
                    if (!showRationale) {
                        dontAskAgain.add(permissions[i]);
                    }
                }
            }

            if(permissionListener != null) {
                permissionListener.result(permissionResult, dontAskAgain);
            }

        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    public void onActivityResult(int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Belvedere.from(this.getContext()).getFilesFromActivityOnResult(requestCode, resultCode, data, new Callback<List<MediaResult>>() {
            @Override
            public void success(List<MediaResult> result) {
                List<MediaResult> filteredMediaResult = new ArrayList<>(result.size());
                for(MediaResult m : result) {
                    if(m.getSize() <= uiConfig.getMaxFileSize() || uiConfig.getMaxFileSize() == -1L) {
                        filteredMediaResult.add(m);
                    }
                }

                if(filteredMediaResult.size() != result.size()) {
                    Toast.makeText(getContext(), uiConfig.getMaxSizeErrorMessage(), Toast.LENGTH_SHORT).show();
                }

                notifyImageSelected(filteredMediaResult, false);

            }
        }, false);
    }

    private boolean isPermissionGranted(String permission) {
        return PermissionUtil.isPermissionGranted(getContext(), permission);
    }

    private void setListener(InternalPermissionCallback listener) {
        this.permissionListener = listener;
    }

    public void handlePermissions(final List<MediaIntent> mediaIntents, final PermissionCallback permissionCallback) {

        final List<String> permissions = new ArrayList<>();
        permissions.addAll(getPermissionsForImageStream());
        permissions.addAll(getPermissionsFromIntents(mediaIntents));

        if(canShowImageStream() && permissions.isEmpty()) {
            permissionCallback.ok(filterMediaIntents(mediaIntents));

        } else if(!canShowImageStream() && permissions.isEmpty()){
            permissionCallback.nope();

        } else {
            askForPermissions(permissions, new InternalPermissionCallback() {
                @Override
                public void result(Map<String, Boolean> permissionResult, List<String> dontAskAgain) {
                    final List<MediaIntent> filteredMediaIntents = filterMediaIntents(mediaIntents);

                    if(canShowImageStream()) {
                        permissionCallback.ok(filteredMediaIntents);
                    } else {
                        permissionCallback.nope();
                    }
                }
            });
        }
    }

    private boolean canShowImageStream() {
        final boolean isBelowKitkat = Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT;
        final boolean hasReadPermission = isPermissionGranted(Manifest.permission.READ_EXTERNAL_STORAGE);

        return isBelowKitkat || hasReadPermission;
    }

    private List<String> getPermissionsForImageStream() {
        final List<String> permissions = new ArrayList<>();

        final boolean isBelowKitkat = Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT;
        final boolean hasReadPermission = isPermissionGranted(Manifest.permission.READ_EXTERNAL_STORAGE);

        if(isBelowKitkat || hasReadPermission) {
            // works

        } else if(!preferences.shouldINeverEverAskForThatPermissionAgain(Manifest.permission.READ_EXTERNAL_STORAGE)) {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE);

        } else {
            //nope

        }

        return permissions;
    }

    private List<String> getPermissionsFromIntents(List<MediaIntent> mediaIntents) {
        final List<String> permission = new ArrayList<>();

        for (MediaIntent intent : mediaIntents) {
            if (!TextUtils.isEmpty(intent.getPermission()) &&
                    !preferences.shouldINeverEverAskForThatPermissionAgain(intent.getPermission()) && intent.isAvailable()) {
                permission.add(intent.getPermission());
            }
        }

        return permission;
    }

    private List<MediaIntent> filterMediaIntents(List<MediaIntent> intents) {
        final List<MediaIntent> filteredMediaIntents = new ArrayList<>();

        for(MediaIntent mediaIntent : intents) {
            if(mediaIntent.isAvailable()) {
                if(TextUtils.isEmpty(mediaIntent.getPermission())) {
                    filteredMediaIntents.add(mediaIntent);
                } else {
                    if(isPermissionGranted(mediaIntent.getPermission())) {
                        filteredMediaIntents.add(mediaIntent);
                    }
                }
            }
        }

        return filteredMediaIntents;
    }

    private void askForPermissions(final List<String> permissions, final InternalPermissionCallback permissionCallback) {
        setListener(new InternalPermissionCallback() {
            @Override
            public void result(Map<String, Boolean> permissionResult, List<String> dontAskAgain) {
                for(String permission : dontAskAgain) {
                    preferences.neverEverAskForThatPermissionAgain(permission);
                }
                permissionCallback.result(permissionResult, dontAskAgain);
                setListener(null);
            }
        });

        if(keyboardHelper.get() != null) {
            keyboardHelper.get().hideKeyboard();
        }

        final String[] strings = permissions.toArray(new String[permissions.size()]);
        requestPermissions(strings, PERMISSION_REQUEST_CODE);
    }

    public KeyboardHelper getKeyboardHelper() {
        return keyboardHelper.get();
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

    public void addListener(Listener listener) {
        imageStreamListener.add(new WeakReference<>(listener));
    }

    public void addScrollListener(ScrollListener listener) {
        imageStreamScrollListener.add(new WeakReference<>(listener));
    }

    void notifyScrollListener(int height, int scrollArea, float scrollPosition) {
        for(WeakReference<ScrollListener> ref : imageStreamScrollListener) {
            final ScrollListener scrollListener = ref.get();
            if(scrollListener != null) {
                scrollListener.onScroll(height, scrollArea, scrollPosition);
            }
        }
    }

    void notifyImageSelected(List<MediaResult> mediaResults, boolean replace) {
        for(WeakReference<Listener> ref : imageStreamListener) {
            final Listener listener = ref.get();
            if(listener != null) {
                listener.onImageSelected(mediaResults, replace);
            }
        }
    }

    void notifyDismissed() {
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

    public void dismiss() {
        if(isAttachmentsPopupVisible()) {
            imageStreamPopup.dismiss();
        }
    }

    public boolean wasOpen() {
        return wasOpen;
    }

    public boolean isAttachmentsPopupVisible() {
        return imageStreamPopup != null;
    }

    interface PermissionCallback {
        void ok(List<MediaIntent> mediaIntents);
        void nope();
    }

    interface InternalPermissionCallback {
        void result(Map<String, Boolean> permissionResult, List<String> dontAskAgain);
    }

    public interface Listener {
        void onDismissed();
        void onVisible();
        void onImageSelected(List<MediaResult> mediaResults, boolean replace);
    }

    public interface ScrollListener {
        void onScroll(int height, int scrollArea, float scrollPosition);
    }
}