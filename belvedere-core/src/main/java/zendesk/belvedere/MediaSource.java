package zendesk.belvedere;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;

import androidx.core.util.Pair;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Internal Helper class. Responsible for creating {@link MediaIntent} and
 * parsing returned data.
 */
class MediaSource {

    private final Storage storage;
    private final IntentRegistry intentRegistry;
    private final Context context;

    MediaSource(Context context, Storage storage, IntentRegistry intentRegistry) {
        this.context = context;
        this.storage = storage;
        this.intentRegistry = intentRegistry;
    }

    /**
     * Create a {@link MediaIntent} that invokes the android document picker or
     * an installed gallery..
     *
     * @return An {@link MediaIntent} or null if this action isn't supported by
     * the system.
     */
    MediaIntent getGalleryIntent(int requestCode,
                                 String contentType,
                                 boolean allowMultiple,
                                 List<String> additionalTypes) {
        if (hasDocumentApp(context)) {
            return new MediaIntent(
                    requestCode,
                    getDocumentAndroidIntent(contentType, allowMultiple, additionalTypes),
                    null,
                    true,
                    MediaIntent.TARGET_DOCUMENT);
        }
        return MediaIntent.notAvailable();
    }

    /**
     * Create a {@link MediaIntent} that invokes an installed camera app.
     *
     * @return An {@link MediaIntent} or null if this action isn't supported by
     *      the system.
     */
    Pair<MediaIntent, MediaResult> getCameraIntent(int requestCode){
        if(canPickImageFromCamera(context)){
            return pickImageFromCameraInternal(context, requestCode);
        }

        return new Pair<>(MediaIntent.notAvailable(), null);
    }

    /**
     * Check if we are able to get an image from an installed camera app. This is a real source of pleasure.
     * <br>
     * We have to check the following things:
     * <ol>
     *     <li>Is there a camera</li>
     *     <li>Is there a camera app</li>
     * </ol>
     *
     * @param context A valid {@link Context}
     * @return {@code true} if it's possible to get an image from camera, {@code false} if not
     */
    private boolean canPickImageFromCamera(Context context){
        return hasCamera(context);
    }

    /**
     * Check if the device is capable to provide images through an
     * installed camera app.
     *
     * <p>
     *      The following things will be checked:
     *      <ol>
     *          <li>Is a camera available</li>
     *          <li>Is there a camera app installed</li>
     * </ol>
     *
     * @param context A valid application {@link Context}
     * @return True if the device allows taking pictures from a camera, false if not
     */
    private boolean hasCamera(Context context){
        final Intent mockIntent = new Intent();
        mockIntent.setAction(MediaStore.ACTION_IMAGE_CAPTURE);

        // ask the system if there is a camera (front, back)
        final PackageManager packageManager = context.getPackageManager();
        final boolean hasCamera = packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA)
                || packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT);

        final boolean hasCameraApp = isIntentResolvable(mockIntent, context);

        L.d(Belvedere.LOG_TAG, String.format(Locale.US, "Camera present: %b, Camera App present: %b", hasCamera, hasCameraApp));

        return (hasCamera && hasCameraApp);
    }

    /**
     * Check if the devices is able to provide media through an installed app.
     *
     * <p>
     *      The following things will be checked:
     *      <ol>
     *          <li>Is an app installed that is able to provided the requested functionality.</li>
     *      </ol>
     *
     * @param context Context
     * @return True if we have permissions to get a picture from a gallery, false if not allowed
     */
    private boolean hasDocumentApp(Context context){
        return isIntentResolvable(getDocumentAndroidIntent("*/*", false, new ArrayList<String>()), context);
    }

    /**
     * Extract data from an {@link Intent}, that was previously requested by an {@link MediaIntent}.
     *
     * @param context A valid application {@link Context}.
     * @param requestCode The requestCode provided by {@link Activity#onActivityResult(int, int, Intent)}
     * @param resultCode The resultCode provided by {@link Activity#onActivityResult(int, int, Intent)}
     * @param data The {@link Intent} provided by {@link Activity#onActivityResult(int, int, Intent)}
     * @param callback Callback that will deliver a list of {@link MediaResult}
     * @param resolveFiles {@code true} if the selected files should be copied into the internal cache,
     *        {@code false} if not
     */
    @SuppressWarnings("JavadocReference")
    void getFilesFromActivityOnResult(Context context, int requestCode, int resultCode,
                                      Intent data, Callback<List<MediaResult>> callback, boolean resolveFiles){
        final List<MediaResult> result = new ArrayList<>();
        final MediaResult belvedereResult = intentRegistry.getForRequestCode(requestCode);

        if(belvedereResult != null) {
            if(belvedereResult.getFile() == null || belvedereResult.getUri() == null) {
                // data in intent
                L.d(Belvedere.LOG_TAG, String.format(Locale.US, "Parsing activity result - Gallery - Ok: %s", (resultCode == Activity.RESULT_OK)));

                if(resultCode == Activity.RESULT_OK) {
                    final List<Uri> uris = extractUrisFromIntent(data);
                    L.d(Belvedere.LOG_TAG, String.format(Locale.US, "Number of items received from gallery: %s", uris.size()));

                    if(resolveFiles) {
                        L.d(Belvedere.LOG_TAG, "Resolving items");
                        ResolveUriTask.start(context, storage, callback, uris);
                        return;

                    } else {
                        L.d(Belvedere.LOG_TAG, "Resolving items turned off");
                        for(Uri uri : uris) {
                            final MediaResult mediaResult = Storage.getMediaResultForUri(context, uri);
                            result.add(mediaResult);
                        }
                    }
                }

            } else {
                // path in registry
                L.d(Belvedere.LOG_TAG, String.format(Locale.US, "Parsing activity result - Camera - Ok: %s", (resultCode == Activity.RESULT_OK)));

                int permission = Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION;
                storage.revokePermissionsFromUri(context, belvedereResult.getUri(), permission);

                if(resultCode == Activity.RESULT_OK){
                    final MediaResult r = Storage.getMediaResultForUri(context, belvedereResult.getUri());
                    result.add(new MediaResult(belvedereResult.getFile(), belvedereResult.getUri(), belvedereResult.getOriginalUri(), belvedereResult.getName(), r.getMimeType(), r.getSize(), r.getWidth(), r.getHeight()));
                    L.d(Belvedere.LOG_TAG, (String.format(Locale.US, "Image from camera: %s", belvedereResult.getFile())));
                }

                intentRegistry.freeSlot(requestCode);
            }
        }

        if(callback != null) {
            callback.internalSuccess(result);
        }
    }

    /**
     * Check if the provided {@link Intent} can be handled by an installed app.
     *
     * @param intent The {@link Intent} to check
     * @param context A valid application {@link Context}
     * @return True if the system is able to handle the provided {@link Intent}
     *      False if not
     */
    private boolean isIntentResolvable(Intent intent, Context context) {
        PackageManager manager = context.getPackageManager();
        List<ResolveInfo> infos = manager.queryIntentActivities(intent, 0);
        return infos.size() > 0;
    }

    /**
     * Extract {@link Uri} from an {@link Intent} that comes back from a gallery
     * or the android document picker.
     *
     * <p>
     *      If the user selects multiple media files, it's necessary to check if
     *      {@link Intent#getClipData()} contains data. Support for selecting multiple
     *      files was introduced with Android Jelly Bean.
     *      {@link android.os.Build.VERSION_CODES#JELLY_BEAN}
     *      <br>
     *      A single selected item is available by calling {@link Intent#getData()}.
     *      <br>
     *      Pretty messed up :/
     * </p>
     *
     * @param intent The returned {@link Intent}, containing {@link Uri} to the selected
     *               media
     * @return A list of {link Uri}
     */
    @SuppressLint("NewApi")
    private List<Uri> extractUrisFromIntent(Intent intent){
        final List<Uri> images = new ArrayList<>();

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN && intent.getClipData() != null) {
            final ClipData clipData = intent.getClipData();

            for (int i = 0, itemCount = clipData.getItemCount(); i < itemCount; i++) {
                final ClipData.Item itemAt = clipData.getItemAt(i);

                if (itemAt.getUri() != null) {
                    images.add(itemAt.getUri());
                }
            }
        } else if(intent.getData() != null){
            images.add(intent.getData());
        }

        return images;
    }

    /**
     * Internal helper method to create an {@link MediaIntent} for opening
     * an installed camera app.
     *
     * @param context A valid application {@link Context}
     * @return An {@link MediaIntent} or null if this action isn't supported by
     *      the system.
     */
    private Pair<MediaIntent, MediaResult> pickImageFromCameraInternal(Context context, int requestCode){

        final File imagePath = storage.getFileForCamera(context);

        if (imagePath == null){
            L.w(Belvedere.LOG_TAG, "Camera Intent: Image path is null. There's something wrong with the storage.");
            return null;
        }

        final Uri uriForFile = storage.getFileProviderUri(context, imagePath);

        if (uriForFile == null) {
            L.w(Belvedere.LOG_TAG, "Camera Intent: Uri to file is null. There's something wrong with the storage or FileProvider configuration.");
            return null;
        }

        L.d(Belvedere.LOG_TAG, String.format(Locale.US, "Camera Intent: Request Id: %s - File: %s - Uri: %s", requestCode, imagePath, uriForFile));

        final Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, uriForFile);
        int permission = Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION;
        storage.grantPermissionsForUri(context, intent, uriForFile, permission);

        /*
            https://code.google.com/p/android/issues/detail?id=188073&q=label%3APriority-Medium&colspec=ID%20Type%20Status%20Owner%20Summary%20Stars&start=100
         */
        final boolean cameraPermissionInManifestButNoGranted =
                PermissionUtil.hasPermissionInManifest(context, Manifest.permission.CAMERA) &&
                !PermissionUtil.isPermissionGranted(context, Manifest.permission.CAMERA);

        final MediaResult r = Storage.getMediaResultForUri(context, uriForFile);
        final MediaResult belvedereResult = new MediaResult(imagePath, uriForFile, uriForFile, imagePath.getName(), r.getMimeType(), r.getSize(), r.getWidth(), r.getHeight());
        final MediaIntent mediaIntent = new MediaIntent(
                requestCode,
                intent,
                cameraPermissionInManifestButNoGranted ? Manifest.permission.CAMERA : null,
                true,
                MediaIntent.TARGET_CAMERA
        );

        return new Pair<>(mediaIntent, belvedereResult);
    }


    /**
     * Internal helper method to create an {@link MediaIntent} for opening
     * an installed gallery app or the android image picker.
     *
     * @return An {@link MediaIntent}
     */
    @TargetApi(Build.VERSION_CODES.KITKAT)
    private Intent getDocumentAndroidIntent(String contentType, boolean allowMultiple, List<String> additionalTypes) {
        final Intent intent;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            L.d(Belvedere.LOG_TAG, "Gallery Intent, using 'ACTION_OPEN_DOCUMENT'");
            intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        } else {
            L.d(Belvedere.LOG_TAG, "Gallery Intent, using 'ACTION_GET_CONTENT'");
            intent = new Intent(Intent.ACTION_GET_CONTENT);
        }

        intent.setType(contentType);
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            // if possible, allow the user to pick multiple images
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, allowMultiple);
        }

        if (additionalTypes != null && !additionalTypes.isEmpty()) {
            intent.putExtra(Intent.EXTRA_MIME_TYPES, additionalTypes.toArray(new String[0]));
        }

        //Intent.createChooser should be used for ACTION_GET_CONTENT with broad mime types like */*.
        //https://developer.android.com/reference/android/content/Intent?hl=ru#ACTION_GET_CONTENT
        //Starting from API 30 it seems like Intent#createChooser is the only way to start a document chooser,
        //even if there's a single default application.
        return isIntentResolvable(intent, context) ?
                intent :
                Intent.createChooser(intent, null);
    }
}