package zendesk.belvedere;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class PermissionManager {

    private static final int PERMISSION_REQUEST_CODE = 9842;

    private InternalPermissionCallback permissionListener = null;

    boolean onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            final Map<String, Boolean> permissionResult = new HashMap<>();

            for(int i = 0, c = permissions.length; i < c; i++) {
                if(grantResults[i] == PackageManager.PERMISSION_GRANTED){
                    permissionResult.put(permissions[i], true);

                } else if(grantResults[i] == PackageManager.PERMISSION_DENIED) {
                    permissionResult.put(permissions[i], false);
                }
            }

            if(permissionListener != null) {
                permissionListener.result(permissionResult);
            }

            return true;
        } else {
            return false;
        }
    }

    void handlePermissions(final Fragment fragment, final List<MediaIntent> mediaIntents, final PermissionCallback permissionCallback) {

        final Context context = fragment.getContext();
        final List<String> permissions = new ArrayList<>();
        permissions.addAll(getPermissionsForImageStream(context));
        permissions.addAll(getPermissionsFromIntents(mediaIntents));

        if(canShowImageStream(context) && permissions.isEmpty()) {
            permissionCallback.onPermissionsGranted(filterMediaIntents(context, mediaIntents));

        } else if(!canShowImageStream(context) && permissions.isEmpty()){
            permissionCallback.onPermissionsDenied();

        } else {
            askForPermissions(fragment, permissions, new InternalPermissionCallback() {
                @Override
                public void result(Map<String, Boolean> permissionResult) {
                    final List<MediaIntent> filteredMediaIntents = filterMediaIntents(context, mediaIntents);

                    if(canShowImageStream(context)) {
                        permissionCallback.onPermissionsGranted(filteredMediaIntents);
                    } else {
                        permissionCallback.onPermissionsDenied();
                    }
                }
            });
        }
    }

    private void askForPermissions(Fragment fragment, final List<String> permissions, final InternalPermissionCallback permissionCallback) {
        setListener(new InternalPermissionCallback() {
            @Override
            public void result(Map<String, Boolean> permissionResult) {
                permissionCallback.result(permissionResult);
                setListener(null);
            }
        });

        final String[] strings = permissions.toArray(new String[permissions.size()]);
        fragment.requestPermissions(strings, PERMISSION_REQUEST_CODE);
    }

    private List<MediaIntent> filterMediaIntents(Context context, List<MediaIntent> intents) {
        final List<MediaIntent> filteredMediaIntents = new ArrayList<>();

        for(MediaIntent mediaIntent : intents) {
            if(mediaIntent.isAvailable()) {
                if(TextUtils.isEmpty(mediaIntent.getPermission())) {
                    filteredMediaIntents.add(mediaIntent);
                } else {
                    if(isPermissionGranted(context, mediaIntent.getPermission())) {
                        filteredMediaIntents.add(mediaIntent);
                    }
                }
            }
        }

        return filteredMediaIntents;
    }

    private List<String> getPermissionsFromIntents(List<MediaIntent> mediaIntents) {
        final List<String> permission = new ArrayList<>();

        for (MediaIntent intent : mediaIntents) {
            if (!TextUtils.isEmpty(intent.getPermission()) && intent.isAvailable()) {
                permission.add(intent.getPermission());
            }
        }

        return permission;
    }

    private List<String> getPermissionsForImageStream(Context context) {
        final List<String> permissions = new ArrayList<>();

        final boolean canShowImageStream = canShowImageStream(context);

        if(!canShowImageStream) {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        }

        return permissions;
    }

    private boolean canShowImageStream(Context context) {
        final boolean isBelowKitkat = Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT;
        final boolean hasReadPermission = isPermissionGranted(context, Manifest.permission.READ_EXTERNAL_STORAGE);

        return isBelowKitkat || hasReadPermission;
    }

    private boolean isPermissionGranted(Context context, String permission) {
        return PermissionUtil.isPermissionGranted(context, permission);
    }

    private void setListener(InternalPermissionCallback listener) {
        this.permissionListener = listener;
    }

    interface PermissionCallback {
        void onPermissionsGranted(List<MediaIntent> mediaIntents);
        void onPermissionsDenied();
    }

    private interface InternalPermissionCallback {
        void result(Map<String, Boolean> permissionResult);
    }

}
