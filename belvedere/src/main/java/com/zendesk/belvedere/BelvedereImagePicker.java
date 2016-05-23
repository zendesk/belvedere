package com.zendesk.belvedere;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Internal Helper class. Responsible for creating {@link BelvedereIntent} and
 * parsing returned data.
 */
class BelvedereImagePicker {

    private final static String LOG_TAG = "BelvedereImagePicker";

    private final BelvedereConfig belvedereConfig;
    private final BelvedereStorage belvedereStorage;

    private final Map<Integer, BelvedereResult> cameraImages = new HashMap<>();

    private final BelvedereLogger log;

    BelvedereImagePicker(final BelvedereConfig belvedereConfig, BelvedereStorage belvedereStorage){
        this.belvedereConfig = belvedereConfig;
        this.belvedereStorage = belvedereStorage;
        this.log = belvedereConfig.getBelvedereLogger();
    }

    /**
     * Get a list of {@link BelvedereIntent}. Each list entry represents
     * an available media source. Sources can be limited due provided configurations, like
     * {@link BelvedereConfig.Builder#withSource(BelvedereSource...)} or due hardware or os
     * specific limitations. Like the absence of a camera or a valid gallery app.
     *
     * @param context A valid application {@link Context}
     * @return A list of {@link BelvedereIntent}
     */
    @NonNull
    List<BelvedereIntent> getBelvedereIntents(@NonNull Context context){
        final TreeSet<BelvedereSource> belvedereSources = belvedereConfig.getBelvedereSources();
        final List<BelvedereIntent> belvedereIntents = new ArrayList<>();

        for(BelvedereSource belvedereSource : belvedereSources){
            BelvedereIntent intent = null;

            switch (belvedereSource){
                case Gallery:
                    intent = getGalleryIntent(context);
                    break;
                case Camera:
                    intent = getCameraIntent(context);
                    break;
            }

            if(intent != null){
                belvedereIntents.add(intent);
            }
        }

        return belvedereIntents;
    }


    /**
     * Create a {@link BelvedereIntent} that invokes the android document picker or
     * an installed gallery, respecting the provided {@link BelvedereConfig}.
     *
     * @param context A valid application {@link Context}.
     * @return An {@link BelvedereIntent} or null if this action isn't supported by
     *      the system.
     */
    @Nullable
    BelvedereIntent getGalleryIntent(@NonNull Context context){
        if(hasGalleryApp(context)) {
            return new BelvedereIntent(getGalleryIntent(), belvedereConfig.getGalleryRequestCode(), BelvedereSource.Gallery, null);
        }
        return null;
    }

    /**
     * Create a {@link BelvedereIntent} that invokes an installed camera app,
     * respecting the provided {@link BelvedereConfig}.
     *
     * @param context A valid application {@link Context}.
     * @return An {@link BelvedereIntent} or null if this action isn't supported by
     *      the system.
     */
    @Nullable
    private BelvedereIntent getCameraIntent(@NonNull Context context){
        if(canPickImageFromCamera(context)){
            return pickImageFromCameraInternal(context);
        }

        return null;
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
    private boolean canPickImageFromCamera(@NonNull Context context){
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
    @NonNull
    private boolean hasCamera(@NonNull Context context){
        final Intent mockIntent = new Intent();
        mockIntent.setAction(MediaStore.ACTION_IMAGE_CAPTURE);

        // ask the system if there is a camera (front, back)
        final PackageManager packageManager = context.getPackageManager();
        final boolean hasCamera = packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA)
                || packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT);

        final boolean hasCameraApp = isIntentResolvable(mockIntent, context);

        log.d(LOG_TAG, String.format(Locale.US, "Camera present: %b, Camera App present: %b", hasCamera, hasCameraApp));

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
    @NonNull
    private boolean hasGalleryApp(@NonNull Context context){
        return isIntentResolvable(getGalleryIntent(), context);
    }

    /**
     * Extract data from an {@link Intent}, that was previously requested by an {@link BelvedereIntent}.
     * It will try its best to extract and resolve the results from {@link #getBelvedereIntents(Context)} (Fragment)}
     *
     * @param context A valid application {@link Context}.
     * @param requestCode The requestCode provided by {@link Activity#onActivityResult(int, int, Intent)}
     * @param resultCode The resultCode provided by {@link Activity#onActivityResult(int, int, Intent)}
     * @param data The {@link Intent} provided by {@link Activity#onActivityResult(int, int, Intent)}
     * @param callback Callback that will deliver a list of {@link BelvedereResult}
     */
    void getFilesFromActivityOnResult(@NonNull Context context, int requestCode, int resultCode, @NonNull Intent data, @Nullable BelvedereCallback<List<BelvedereResult>> callback){
        final List<BelvedereResult> result = new ArrayList<>();

        if(requestCode == belvedereConfig.getGalleryRequestCode()){
            log.d(LOG_TAG, String.format(Locale.US, "Parsing activity result - Gallery - Ok: %s", (resultCode == Activity.RESULT_OK)));

            if(resultCode == Activity.RESULT_OK) {
                final List<Uri> uris = extractUrisFromIntent(data);
                log.d(LOG_TAG, String.format(Locale.US, "Number of items received from gallery: %s", uris.size()));
                new BelvedereResolveUriTask(context, log, belvedereStorage, callback).execute(uris.toArray(new Uri[uris.size()]));
                return;
            }

        }else if(cameraImages.containsKey(requestCode)){
            log.d(LOG_TAG, String.format(Locale.US, "Parsing activity result - Camera - Ok: %s", (resultCode == Activity.RESULT_OK)));

            final BelvedereResult belvedereResult = cameraImages.get(requestCode);
            belvedereStorage.revokePermissionsFromUri(context, belvedereResult.getUri(),
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);

            if(resultCode == Activity.RESULT_OK){
                result.add(belvedereResult);
                log.d(LOG_TAG, (String.format(Locale.US, "Image from camera: %s", belvedereResult.getFile())));
            }

            cameraImages.remove(requestCode);
        }

        if(callback != null) {
            callback.internalSuccess(result);
        }
    }

    /**
     * Check if at least one requested {@link BelvedereSource}
     * is available.
     *
     * @param context A valid application {@link Context}
     * @return {@code true} if one or more sources are available,
     *         {@code false} if no source is available.
     */
    public boolean oneOrMoreSourceAvailable(@NonNull final Context context){
        for(BelvedereSource s : BelvedereSource.values()){
            if(isFunctionalityAvailable(s, context)){
                return true;
            }
        }
        return false;
    }

    /**
     * Check if a specific {@link BelvedereSource} is available.
     *
     * @param source The {@link BelvedereSource} to check.
     * @param context A valid application {@link Context}
     * @return {@code true} if the source is available, {@code false} if not.
     */
    public boolean isFunctionalityAvailable(@NonNull final BelvedereSource source, @NonNull final Context context) {
        final TreeSet<BelvedereSource> belvedereSources = belvedereConfig.getBelvedereSources();

        if(!belvedereSources.contains(source)){
            return false;
        }

        switch (source){
            case Camera:
                return canPickImageFromCamera(context);
            case Gallery:
                return hasGalleryApp(context);
            default:
                return false;
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
    @NonNull
    private boolean isIntentResolvable(@NonNull Intent intent, @NonNull Context context){
        return intent.resolveActivity(context.getPackageManager()) != null;
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
    @NonNull
    @SuppressLint("NewApi")
    private List<Uri> extractUrisFromIntent(@NonNull Intent intent){
        final List<Uri> images = new ArrayList<>();

        /*
            Support for selecting multiple images starts with JellyBean. Selected images
            were attached to an Intent as ClipData
         */
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN && intent.getClipData() != null){
            final ClipData clipData = intent.getClipData();

            for(int i = 0, itemCount = clipData.getItemCount(); i < itemCount; i++){
                final ClipData.Item itemAt = clipData.getItemAt(i);

                if(itemAt.getUri() != null){
                    images.add(itemAt.getUri());
                }
            }
        }
        else if(intent.getData() != null){
            images.add(intent.getData());
        }

        return images;
    }

    /**
     * Internal helper method to create an {@link BelvedereIntent} for opening
     * an installed camera app.
     *
     * @param context A valid application {@link Context}
     * @return An {@link BelvedereIntent} or null if this action isn't supported by
     *      the system.
     */
    @Nullable
    private BelvedereIntent pickImageFromCameraInternal(@NonNull Context context){
         /*
            To get an image from a camera app have to provide a file, pointing to the place where the image should be stored
            We keep in track of all new images with a Map and requestIds
         */
        final Set<Integer> integers = cameraImages.keySet();

        int requestId = belvedereConfig.getCameraRequestCodeEnd();
        for(int i = belvedereConfig.getCameraRequestCodeStart(); i < belvedereConfig.getCameraRequestCodeEnd(); i++){
            if(!integers.contains(i)){
                requestId = i;
                break;
            }
        }

        final File imagePath = belvedereStorage.getFileForCamera(context);

        if(imagePath == null){
            log.w(LOG_TAG, "Camera Intent: Image path is null. There's something wrong with the storage.");
            return null;
        }

        final Uri uriForFile = belvedereStorage.getFileProviderUri(context, imagePath);

        if(uriForFile == null) {
            log.w(LOG_TAG, "Camera Intent: Uri to file is null. There's something wrong with the storage or FileProvider configuration.");
            return null;
        }

        cameraImages.put(requestId, new BelvedereResult(imagePath, uriForFile));

        log.d(LOG_TAG, String.format(Locale.US, "Camera Intent: Request Id: %s - File: %s - Uri: %s", requestId, imagePath, uriForFile));

        final Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, uriForFile);
        belvedereStorage.grantPermissionsForUri(context, intent, uriForFile,
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);

        /*
            https://code.google.com/p/android/issues/detail?id=188073&q=label%3APriority-Medium&colspec=ID%20Type%20Status%20Owner%20Summary%20Stars&start=100
         */
        final boolean cameraPermissionInManifestButNoGranted =
                PermissionUtil.hasPermissionInManifest(context, Manifest.permission.CAMERA) &&
                !PermissionUtil.isPermissionGranted(context, Manifest.permission.CAMERA);

        return new BelvedereIntent(
                intent,
                requestId,
                BelvedereSource.Camera,
                cameraPermissionInManifestButNoGranted ? Manifest.permission.CAMERA : null
        );
    }


    /**
     * Internal helper method to create an {@link BelvedereIntent} for opening
     * an installed gallery app or the android image picker.
     *
     * @return An {@link BelvedereIntent}
     */
    @TargetApi(Build.VERSION_CODES.KITKAT)
    @NonNull
    private Intent getGalleryIntent(){
        final Intent intent;

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            log.d(LOG_TAG, "Gallery Intent, using 'ACTION_OPEN_DOCUMENT'");
            intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        } else {
            log.d(LOG_TAG, "Gallery Intent, using 'ACTION_GET_CONTENT'");
            intent = new Intent(Intent.ACTION_GET_CONTENT);
        }

        intent.setType(belvedereConfig.getContentType());
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2){
            // if possible, allow the user to pick multiple images
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, belvedereConfig.allowMultiple());
        }

        return intent;
    }
}