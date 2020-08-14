package zendesk.belvedere;

import android.os.Build;

/**
 * Implementation which returns the same value as {@link android.os.Build.VERSION#SDK_INT}
 */
public class DefaultBuildVersionProvider implements BuildVersionProvider {
    @Override
    public int currentVersion() {
        return Build.VERSION.SDK_INT;
    }
}
