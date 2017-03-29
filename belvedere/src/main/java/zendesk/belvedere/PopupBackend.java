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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PopupBackend extends Fragment {

    private static final int PERMISSION_REQUEST_CODE = 9842;

    private PermissionStorage preferences;

    private WeakReference<KeyboardHelper> keyboardHelper = new WeakReference<>(null);
    private WeakReference<ImageStreamPopup> imageStreamPopupWeakReference = new WeakReference<ImageStreamPopup>(null);
    private InternalPermissionCallback permissionListener = null;

    private WeakReference<ImageStreamPopup.Listener> imageStreamListener = new WeakReference<>(null);

    private boolean wasOpen = false;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferences = new PermissionStorage(getContext());
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        setRetainInstance(true);
        return null;
    }

    @Override
    public void onPause() {
        super.onPause();
        final ImageStreamPopup imageStreamPopup = imageStreamPopupWeakReference.get();
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
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Belvedere.from(this.getContext()).getFilesFromActivityOnResult(requestCode, resultCode, data, new Callback<List<MediaResult>>() {
            @Override
            public void success(List<MediaResult> result) {
                if(imageStreamListener.get() != null) {
                    imageStreamListener.get().onImageSelected(result);
                }
            }
        });
    }

    private boolean isPermissionGranted(String permission) {
        return PermissionUtil.isPermissionGranted(getContext(), permission);
    }

    private void askForPermission(List<String> permission) {
        final String[] strings = permission.toArray(new String[permission.size()]);
        requestPermissions(strings, PERMISSION_REQUEST_CODE);
    }

    private void setListener(InternalPermissionCallback listener) {
        this.permissionListener = listener;
    }

    public void handlePermissionStuffForStream(final List<MediaIntent> mediaIntents, final PermissionCallback permissionCallback) {

        final List<String> permissions = new ArrayList<>();
        permissions.addAll(getPermissionsForImageStream());
        permissions.addAll(getPermissionsFromIntents(mediaIntents));

        if(canShowImageStream() && permissions.isEmpty()) {
            permissionCallback.ok(filterMediaIntents(mediaIntents));

        } else if(!canShowImageStream() && permissions.isEmpty()){
            permissionCallback.nope();

        } else {
            handlePermissionStuff(permissions, new InternalPermissionCallback() {
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

    private void handlePermissionStuff(final List<String> permissions, final InternalPermissionCallback permissionCallback) {

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

        askForPermission(permissions);

    }

    public KeyboardHelper getKeyboardHelper() {
        return keyboardHelper.get();
    }

    public void setKeyboardHelper(KeyboardHelper keyboardHelper) {
        this.keyboardHelper = new WeakReference<>(keyboardHelper);
    }

    public void setImageStreamPopup(ImageStreamPopup imageStreamPopup) {
        this.imageStreamPopupWeakReference = new WeakReference<>(imageStreamPopup);
    }

    public void setImageStreamListener(ImageStreamPopup.Listener listener) {
        this.imageStreamListener = new WeakReference<>(listener);
    }

    ImageStreamPopup.Listener getImListener() {
        return imageStreamListener.get();
    }

    public boolean wasOpen() {
        return wasOpen;
    }

    interface PermissionCallback {
        void ok(List<MediaIntent> mediaIntents);
        void nope();
    }

    interface InternalPermissionCallback {
        void result(Map<String, Boolean> permissionResult, List<String> dontAskAgain);
    }

}

