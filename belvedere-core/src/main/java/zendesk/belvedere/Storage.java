package zendesk.belvedere;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.MimeTypeMap;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Internal helper class. Responsible for creating files
 * and handling the {@link BelvedereFileProvider}.
 */
class Storage {

    private final static String LOG_TAG = "BelvedereStorage";

    private final static String CAMERA_IMAGE_DIR = "camera";
    private final static String GALLERY_IMAGE_DIR = "gallery";
    private final static String REQUEST_IMAGE_DIR = "request";

    private final static String ATTACHMENT_NAME = "attachment_%s";
    private final static String CAMERA_IMG_NAME = "camera_image_%s";
    private final static String CAMERA_IMG_SUFFIX = ".jpg";
    private final static String CAMERA_DATETIME_STRING_FORMAT = "yyyyMMddHHmmssSSS";

    private Logger log;
    private String directoryName;

    Storage(String directoryName, Logger log){
        this.directoryName = directoryName;
        this.log = log;
    }

    /**
     * Grant all Apps that are resolved through the provided {@link Intent} permissions to the file
     * behind the provided {@link Uri}
     *
     * @param context A valid application {@link Context}
     * @param intent An {@link Intent}
     * @param uri An {@link Uri} to a file, managed by our {@link BelvedereFileProvider}
     * @param permission Permission that should be granted to the Apps, opened by the provided Intent
     */
    void grantPermissionsForUri(Context context, Intent intent, Uri uri, int permission){
        List<ResolveInfo> resInfoList = context.getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
        for (ResolveInfo resolveInfo : resInfoList) {
            String packageName = resolveInfo.activityInfo.packageName;
            context.grantUriPermission(packageName, uri, permission);
        }
    }

    /**
     * Revoke the specified permission from an {@link Uri}.
     *
     * @param context A valid application {@link Context}
     * @param uri An {@link Uri} to a file, managed by our {@link BelvedereFileProvider}
     * @param permission Permissions that should be revoked
     */
    void revokePermissionsFromUri(Context context, Uri uri, int permission){
        context.revokeUriPermission(uri, permission);
    }

    /**
     * Create an {@link Uri} that points to the specified {@link File}. The {@link File} must be
     * accessible through our {@link BelvedereFileProvider}
     *
     * @param context A valid application {@link Context}
     * @param file A {@link File}, accessible through our {@link BelvedereFileProvider}
     * @return The {@link Uri} pointing to the provided File.
     */
    Uri getFileProviderUri(Context context, File file){
        final String authority = getFileProviderAuthority(context);

        try {
            return FileProvider.getUriForFile(context, authority, file);

        } catch(IllegalArgumentException e){
            log.e(LOG_TAG, String.format(Locale.US, "The selected file can't be shared %s", file.toString()));
            return null;

        } catch(NullPointerException e){

            final String msg = String.format(Locale.US,
                    "=====================\n" +
                    "FileProvider failed to retrieve file uri. There might be an issue with the FileProvider \n" +
                    "Please make sure that manifest-merger is working, and that you have defined the applicationId (package name) in the build.gradle\n" +
                    "Manifest merger: http://tools.android.com/tech-docs/new-build-system/user-guide/manifest-merger\n" +
                    "If your are not able to use gradle or the manifest merger, please add the following to your AndroidManifest.xml:\n" +
                    "        <provider\n" +
                    "            android:name=\"com.zendesk.belvedere.BelvedereFileProvider\"\n" +
                    "            android:authorities=\"${applicationId}${belvedereFileProviderAuthoritySuffix}\"\n" +
                    "            android:exported=\"false\"\n" +
                    "            android:grantUriPermissions=\"true\">\n" +
                    "            <meta-data\n" +
                    "                android:name=\"android.support.FILE_PROVIDER_PATHS\"\n" +
                    "                android:resource=\"@xml/belvedere_attachment_storage_v2\" />\n" +
                    "        </provider>\n" +
                    "=====================",
                    authority);

            Log.e(LOG_TAG, msg, e);
            log.e(LOG_TAG, msg, e);

            throw new RuntimeException("Please specify your application id");
        }
    }

    /**
     * Get the authority of {@link BelvedereFileProvider}, that is used to access files.
     * <br>
     * Defined in the AndroidManifest as android:authorities=".."
     *
     * @param context A valid application {@link Context}
     * @return The authority as a {@link String}
     */
    String getFileProviderAuthority(Context context){
        final String suffix = context.getString(R.string.belvedere_sdk_fpa_suffix);
        return String.format(Locale.US, "%s%s", context.getPackageName(), suffix);
    }

    /**
     * Get a {@link File} for a newly created image by the camera.
     *
     * @param context A valid application {@link Context}
     * @return The {@link File}
     */
    File getFileForCamera(Context context){
        final File cacheDir = getAttachmentDir(context, CAMERA_IMAGE_DIR);

        if(cacheDir == null){
            log.w(LOG_TAG, "Error creating cache directory");
            return null;
        }

        final SimpleDateFormat sdf = new SimpleDateFormat(CAMERA_DATETIME_STRING_FORMAT, Locale.US);
        final String fileName = String.format(Locale.US, CAMERA_IMG_NAME, sdf.format(new Date(System.currentTimeMillis())));

        return createTempFile(fileName, CAMERA_IMG_SUFFIX, cacheDir);
    }

    /**
     * Get a {@link File} for media from the gallery.
     *
     * @param context A valid application {@link Context}
     * @param uri {@link Uri} to the media from the gallery
     * @return The {@link File}
     */
    File getTempFileForGalleryImage(Context context, Uri uri) {
        final File cacheDir = getAttachmentDir(context, GALLERY_IMAGE_DIR);

        if(cacheDir == null){
            log.w(LOG_TAG, "Error creating cache directory");
            return null;
        }

        String fileName = getFileNameFromUri(context, uri);
        String suffix = null;

        if(TextUtils.isEmpty(fileName)){
            final SimpleDateFormat sdf = new SimpleDateFormat(CAMERA_DATETIME_STRING_FORMAT, Locale.US);
            fileName = String.format(Locale.US, ATTACHMENT_NAME, sdf.format(new Date(System.currentTimeMillis())));
            suffix = getExtension(context, uri);
        }

        return createTempFile(fileName, suffix, cacheDir);
    }

    /**
     * Get a {@link File} for storing misc data that should be
     * accessible for {@link BelvedereFileProvider}. If the cache dir doesn't
     * exist we try to create it.
     *
     * @param context A valid application {@link Context}.
     * @param fileName The name of the file.
     * @return The File.
     */
    File getTempFileForRequestAttachment(Context context, String fileName) {
        final File cacheDir = getAttachmentDir(context, REQUEST_IMAGE_DIR);

        if (cacheDir == null) {
            log.w(LOG_TAG, "Error creating cache directory");
            return null;
        }

        return createTempFile(fileName, null, cacheDir);
    }

    /**
     * Clear the Belvedere cache.
     *
     * @param context A valid application {@link Context}.
     */
    void clearStorage(Context context) {
        final File rootDir = new File(getRootDir(context) + File.separator + directoryName);
        if(rootDir.isDirectory()){
            clearDirectory(rootDir);
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void clearDirectory(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory()){
            for (File child : fileOrDirectory.listFiles()){
                clearDirectory(child);
            }
        }
        fileOrDirectory.delete();
    }

    /**
     * Create a {@link File} with the given parameters.
     *
     * <p>
     *     Suffix could be null. If a suffice is provided
     *     it has to start with a '.'. E.g. '.gif'
     * </p>
     *
     * @param fileName The name of the {@link File}.
     * @param suffix The suffix of the {@link File}.
     * @param dir The directory, where the {@link File} should live.
     * @return The {@link File}
     */
    private File createTempFile(String fileName, String suffix, File dir){
        return new File(dir, fileName + (!TextUtils.isEmpty(suffix) ? suffix : ""));
    }

    /**
     * Get and create a sub directory in the Belvedere cache.
     * <p>
     *     If the directory not exist the method tries to create it.
     * </p>
     *
     * @param context A valid application {@link Context}
     * @param subDirectory The name of the sub directory
     * @return A {@link File} pointing to the directory.
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    private File getAttachmentDir(Context context, String subDirectory){

        final String subDirectoryString;
        if(!TextUtils.isEmpty(subDirectory)){
            subDirectoryString = subDirectory + File.separator;
        } else {
            subDirectoryString = "";
        }

        final File dir = new File(getRootDir(context) + File.separator + directoryName + File.separator + subDirectoryString);

        if(!dir.isDirectory()){
            dir.mkdirs();
        }

        return dir.isDirectory() ? dir : null;
    }


    /**
     * Get the root directory of the Belvedere cache.
     * <p>
     *     Belvedere is using the internal cache directory
     *     to store stuff. ({@link Context#getCacheDir()})
     * </p>
     *
     * @param context A valid application {@link Context}
     * @return A {@link File} pointing to the directory, where
     *      the Belvedere cache should live,
     */
    private String getRootDir(Context context) {
        return context.getCacheDir().getAbsolutePath();
    }

    /**
     * Try to guess the mime type of the media {@link File} behind
     * the provided {@link Uri}.
     * <p>
     *     The returned result will look like this '.gif' or '.jpg'.
     *     <br>
     *     If this method isn't able to figure out the mime type, it will
     *     return '.tmp'
     * </p>
     *
     * @param context A valid application {@link Context}
     * @param uri An {@link Uri}
     * @return The mime type as a {@link String}.
     */
    private String getExtension(Context context, Uri uri){
        final MimeTypeMap mime = MimeTypeMap.getSingleton();
        final String schema = uri.getScheme();
        String ext = "tmp";

        if(ContentResolver.SCHEME_CONTENT.equals(schema)) {
            final ContentResolver cr = context.getContentResolver();
            ext = mime.getExtensionFromMimeType(cr.getType(uri));

        } else if(ContentResolver.SCHEME_FILE.equals(schema)) {
            final String fullFileName = uri.getLastPathSegment();
            final int i = fullFileName.lastIndexOf(".");

            if(i != -1) {
                ext = fullFileName.substring(i, fullFileName.length());
            }
        }

        return String.format(Locale.US, ".%s", ext);
    }

    /**
     * Try to get the file name of a {@link File} behind an {@link Uri}.
     * This method queries {@link ContentResolver} for the
     * {@link MediaStore.MediaColumns#DISPLAY_NAME} column the find the name of
     * the provided {@link Uri}.
     *
     * @param context A valid application {@link Context}
     * @param uri Link to the {@link Uri}
     * @return The name of the {@link File}, or an empty {@link String}
     */
    private String getFileNameFromUri(Context context, Uri uri){
        final String schema = uri.getScheme();
        String path = "";

        if(ContentResolver.SCHEME_CONTENT.equals(schema)) {
            final String[] projection = {MediaStore.MediaColumns.DISPLAY_NAME};
            final ContentResolver contentResolver = context.getContentResolver();
            final Cursor cursor = contentResolver.query(uri, projection, null, null, null);


            if (cursor != null) {
                try {
                    if (cursor.moveToFirst()) {
                        path = cursor.getString(0);
                    }
                } finally {
                    cursor.close();
                }
            }

        } else if(ContentResolver.SCHEME_FILE.equals(schema)) {
            path = uri.getLastPathSegment();

        }

        return path;
    }
}