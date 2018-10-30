package zendesk.belvedere;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import androidx.core.content.ContextCompat;

/**
 * Set of helpers around Android-M permissions.
 */
class PermissionUtil {

    /**
     * Check if the user has granted the specified permission.
     *
     * @param context A valid application {@link Context}
     * @param permissionName Name of the permission
     * @return {@code true} if the permission was granted, {@code false} if not
     */
    static boolean isPermissionGranted(Context context, String permissionName) {
        return ContextCompat.checkSelfPermission(context, permissionName) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Internal helper method for checking, if a permission is declared in the
     * glorious {@code AndroidManifest.xml}.
     *
     * @param context A valid application {@link Context}
     * @param permissionName Name of the permission
     * @return {@code true} if permissions is declared, {@code false} if not
     */
    static boolean hasPermissionInManifest(Context context, String permissionName) {
        final String packageName = context.getPackageName();
        try {
            final PackageInfo packageInfo = context.getPackageManager()
                    .getPackageInfo(packageName, PackageManager.GET_PERMISSIONS);
            final String[] declaredPermissions = packageInfo.requestedPermissions;
            if (declaredPermissions != null && declaredPermissions.length > 0) {
                for (String p : declaredPermissions) {
                    if (p.equals(permissionName)) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            // intentionally empty
        }

        return false;
    }
}