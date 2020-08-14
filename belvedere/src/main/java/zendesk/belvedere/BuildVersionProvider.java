package zendesk.belvedere;

/**
 * Provides the build version of the current Android device
 */
interface BuildVersionProvider {

    /**
     * Returns the build version of the current android device. Usually this will be {@link android.os.Build.VERSION#SDK_INT}.
     *
     * @return the build version of the current Android device
     */
    int currentVersion();
}
