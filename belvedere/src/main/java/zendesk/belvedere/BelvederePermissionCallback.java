package zendesk.belvedere;

import java.util.List;

/**
 * Permission Callback used to trigger the granted and denied runtime permissions.
 *
 * <p>
 * This interface can be helpful for integrators if they want to build a custom logic that depends on the granted
 * and denied runtime permissions.
 * Like, building a SnackBar or a Dialog to let the user navigate to application settings in case of the user denied
 * the runtime permissions more than once.
 * </p>
 */
public interface BelvederePermissionCallback {

    /**
     * Callback that will be fired once the user granted the runtime permissions.
     *
     * @param mediaIntents They are the {@link MediaIntent} that the user granted.
     */
    void onPermissionsGranted(List<MediaIntent> mediaIntents);

    /**
     * Callback that will be fired once the user denied the runtime permissions.
     *
     * @param isMoreThanOnce It is a boolean value that will be true if the user denied the runtime permission only
     *                       once, otherwise false.
     */
    void onPermissionsDenied(boolean isMoreThanOnce);

}
