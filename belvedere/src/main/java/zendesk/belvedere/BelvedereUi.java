package zendesk.belvedere;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.view.ViewGroup;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import zendesk.belvedere.ui.R;

/**
 * Main entry-point for interacting the UI components of Belvedere.
 *
 * There are two different UIs available:
 *  - Dialog (from 1.x)
 *  - ImageStream (BottomSheet)
 */
public class BelvedereUi {

    private final static String FRAGMENT_TAG = "BelvedereDialog";
    private final static String EXTRA_MEDIA_INTENT = "extra_intent";
    private final static String FRAGMENT_TAG_POPUP = "belvedere_image_stream";

    /**
     * Gets the builder for showing the ImageStream.
     */
    public static ImageStreamBuilder imageStream(@NonNull Context context) {
        return new ImageStreamBuilder(context);
    }

    /**
     * Install the ImageStream to an {@link AppCompatActivity}
     *
     * @param activity the activity that will show the ImageStream
     * @return an {@link ImageStream}
     */
    public static ImageStream install(@NonNull AppCompatActivity activity) {
        final FragmentManager supportFragmentManager = activity.getSupportFragmentManager();
        final Fragment fragment = supportFragmentManager.findFragmentByTag(FRAGMENT_TAG_POPUP);

        final ImageStream popupBackend;
        if(fragment instanceof ImageStream) {
            popupBackend = (ImageStream) fragment;
        } else {
            popupBackend = new ImageStream();
            supportFragmentManager
                    .beginTransaction()
                    .add(popupBackend, FRAGMENT_TAG_POPUP)
                    .commit();
        }

        popupBackend.setKeyboardHelper(KeyboardHelper.inject(activity));
        return popupBackend;
    }

    public static class ImageStreamBuilder {

        private final Context context;
        private boolean resolveMedia = true;
        private List<MediaIntent> mediaIntents = new ArrayList<>();
        private List<MediaResult> selectedItems = new ArrayList<>();
        private List<MediaResult> extraItems = new ArrayList<>();
        private List<Integer> touchableItems = new ArrayList<>();
        private long maxFileSize = -1L;
        private boolean fullScreenOnly = false;

        private ImageStreamBuilder(Context context){
            this.context = context;
        }

        /**
         * Allow the user to select an image from the camera.
         */
        public ImageStreamBuilder withCameraIntent() {
            final MediaIntent cameraIntent = Belvedere.from(context).camera().build();
            this.mediaIntents.add(cameraIntent);
            return this;
        }

        /**
         * Allow the user to select files of the specified content type from the system. Only one
         * of the following should be called as they are mutually exclusive:
         *
         * <li>{@link ImageStreamBuilder#withDocumentIntent(String, boolean)}</li>
         * <li>{@link ImageStreamBuilder#withDocumentIntent(List, boolean)}</li>
         *
         * @param contentType restrict the files to a content type
         * @param allowMultiple allow the user to select multiple attachments in a third party app or the system file picker
         */
        public ImageStreamBuilder withDocumentIntent(@NonNull String contentType, boolean allowMultiple) {
            final MediaIntent mediaIntent = Belvedere.from(context)
                    .document()
                    .allowMultiple(allowMultiple)
                    .contentType(contentType)
                    .build();
            this.mediaIntents.add(mediaIntent);
            return this;
        }

        /**
         * Allow the user to select files of any specified content type from the system. This can
         * be used when allowing the selection of files from a disjoint set (e.g. "image&#47;*" and
         * "text&#47;*"). Only one of the following should be called as they are mutually exclusive:
         *
         * <li>{@link ImageStreamBuilder#withDocumentIntent(String, boolean)}</li>
         * <li>{@link ImageStreamBuilder#withDocumentIntent(List, boolean)}</li>
         *
         * @param contentTypes restrict the files to the content types
         * @param allowMultiple allow the user to select multiple attachments in a third party app or the system file picker
         */
        public ImageStreamBuilder withDocumentIntent(@NonNull List<String> contentTypes, boolean allowMultiple) {
            final MediaIntent mediaIntent = Belvedere.from(context)
                    .document()
                    .allowMultiple(allowMultiple)
                    .contentTypes(contentTypes)
                    .build();
            this.mediaIntents.add(mediaIntent);
            return this;
        }

        /**
         * Pass in files that are should be marked as selected.
         */
        public ImageStreamBuilder withSelectedItems(List<MediaResult> mediaResults) {
            this.selectedItems = new ArrayList<>(mediaResults);
            return this;
        }

        /**
         * Pass in files that are not selected but should show up in the ImageStream.
         */
        public ImageStreamBuilder withExtraItems(List<MediaResult> mediaResults) {
            this.extraItems = new ArrayList<>(mediaResults);
            return this;
        }

        /**
         * Specify a list of ids from your activity that should be clickable
         * although the ImageStream is visible.
         */
        public ImageStreamBuilder withTouchableItems(@IdRes int... ids) {
            final List<Integer> objects = new ArrayList<>(ids.length);
            for(int id : ids) {
                objects.add(id);
            }
            this.touchableItems = objects;
            return this;
        }

        /**
         * Define a maximum file size. Files bigger than the provided value are not selectable.
         *
         * @param maxFileSize maximum file size in bytes
         */
        public ImageStreamBuilder withMaxFileSize(long maxFileSize) {
            this.maxFileSize = maxFileSize;
            return this;
        }

        /**
         * Always show the image picker in full screen.
         *
         * @param enabled {@code true} if the picker should be shown full screen to the user, {@code false}
         *                 if the picker should be drawn above the keyboard
         */
        public ImageStreamBuilder withFullScreenOnly(boolean enabled) {
            this.fullScreenOnly = enabled;
            return this;
        }

        /**
         * Show the ImageStream to the user.
         */
        public void showPopup(AppCompatActivity activity) {
            final ImageStream popupBackend = BelvedereUi.install(activity);

            popupBackend.handlePermissions(mediaIntents, new PermissionManager.PermissionCallback() {
                @Override
                public void onPermissionsGranted(final List<MediaIntent> mediaIntents) {
                    final Activity appCompatActivity = popupBackend.getActivity();

                    if(appCompatActivity != null && !appCompatActivity.isChangingConfigurations()) {
                        final ViewGroup decorView = (ViewGroup) appCompatActivity.getWindow().getDecorView();
                        decorView.post(new Runnable() {
                            @Override
                            public void run() {
                                final UiConfig uiConfig = new UiConfig(mediaIntents, selectedItems, extraItems, resolveMedia, touchableItems, maxFileSize, fullScreenOnly);
                                final ImageStreamUi show = ImageStreamUi.show(
                                        appCompatActivity,
                                        decorView,
                                        popupBackend,
                                        uiConfig);
                                popupBackend.setImageStreamUi(show, uiConfig);
                            }
                        });
                    }
                }

                @Override
                public void onPermissionsDenied() {
                    Activity appCompatActivity = popupBackend.getActivity();
                    if(appCompatActivity != null) {
                        Toast.makeText(appCompatActivity, R.string.belvedere_permissions_denied, Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }

    /**
     * Show the Belvedere dialog to the user
     *
     * @param fm a valid {@link FragmentManager}
     * @param mediaIntent a list of {@link MediaIntent}
     */
    public static void showDialog(FragmentManager fm, List<MediaIntent> mediaIntent) {
        if (mediaIntent == null || mediaIntent.size() == 0) {
            return;
        }

        final BelvedereDialog dialog = new BelvedereDialog();
        dialog.setArguments(getBundle(mediaIntent, new ArrayList<MediaResult>(0), new ArrayList<MediaResult>(0), true, new ArrayList<Integer>(0)));
        dialog.show(fm, FRAGMENT_TAG);
    }

    /**
     * Show the Belvedere dialog to the user
     *
     * @param fm a valid {@link FragmentManager}
     * @param mediaIntent a list of {@link MediaIntent}
     */
    public static void showDialog(FragmentManager fm, MediaIntent... mediaIntent) {
        if (mediaIntent == null || mediaIntent.length == 0) {
            return;
        }

        showDialog(fm, Arrays.asList(mediaIntent));
    }

    private static Bundle getBundle(List<MediaIntent> mediaIntent, List<MediaResult> selectedItems,
                                    List<MediaResult> extraItems, boolean resolveMedia,
                                    List<Integer> touchableIds) {

        final List<MediaIntent> intents = new ArrayList<>();
        final List<MediaResult> selected = new ArrayList<>();
        final List<MediaResult> extra = new ArrayList<>();

        if(mediaIntent != null) {
            intents.addAll(mediaIntent);
        }

        if(selectedItems != null) {
            selected.addAll(selectedItems);
        }

        if(extraItems != null) {
            extra.addAll(extraItems);
        }

        final UiConfig uiConfig = new UiConfig(intents, selected, extra, resolveMedia, touchableIds, -1L, false);
        final Bundle bundle = new Bundle();
        bundle.putParcelable(EXTRA_MEDIA_INTENT, uiConfig);

        return bundle;
    }

    static UiConfig getUiConfig(Bundle bundle) {
        UiConfig config = bundle.getParcelable(EXTRA_MEDIA_INTENT);

        if (config == null) {
            return new UiConfig();
        }

        return config;
    }

    public static class UiConfig implements Parcelable {

        private final List<MediaIntent> intents;
        private final List<MediaResult> selectedItems;
        private final List<MediaResult> extraItems;
        private final List<Integer> touchableElements;
        private final boolean resolveMedia;
        private final long maxFileSize;
        private final boolean fullScreenOnly;

        UiConfig() {
            this.intents = new ArrayList<>();
            this.selectedItems = new ArrayList<>();
            this.extraItems = new ArrayList<>();
            this.touchableElements = new ArrayList<>();
            this.resolveMedia = true;
            this.maxFileSize = -1L;
            this.fullScreenOnly = false;
        }

        UiConfig(List<MediaIntent> intents, List<MediaResult> selectedItems,
                 List<MediaResult> extraItems, boolean resolveMedia,
                 List<Integer> touchableElements, long maxFileSize,
                 boolean fullScreenOnly) {
            this.intents = intents;
            this.selectedItems = selectedItems;
            this.extraItems = extraItems;
            this.resolveMedia = resolveMedia;
            this.touchableElements = touchableElements;
            this.maxFileSize = maxFileSize;
            this.fullScreenOnly = fullScreenOnly;
        }

        UiConfig(Parcel in) {
            this.intents = in.createTypedArrayList(MediaIntent.CREATOR);
            this.selectedItems = in.createTypedArrayList(MediaResult.CREATOR);
            this.extraItems = in.createTypedArrayList(MediaResult.CREATOR);
            this.touchableElements = new ArrayList<>();
            in.readList(touchableElements, Integer.class.getClassLoader());
            this.resolveMedia = in.readInt() == 1;
            this.maxFileSize = in.readLong();
            this.fullScreenOnly = in.readInt() == 1;
        }

        List<MediaIntent> getIntents() {
            return intents;
        }

        List<MediaResult> getSelectedItems() {
            return selectedItems;
        }

        List<MediaResult> getExtraItems() {
            return extraItems;
        }

        List<Integer> getTouchableElements() {
            return touchableElements;
        }

        long getMaxFileSize() {
            return maxFileSize;
        }

        boolean showFullScreenOnly() {
            return fullScreenOnly;
        }

        public static final Creator<UiConfig> CREATOR = new Creator<UiConfig>() {
            @Override
            public UiConfig createFromParcel(Parcel in) {
                return new UiConfig(in);
            }

            @Override
            public UiConfig[] newArray(int size) {
                return new UiConfig[size];
            }
        };

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeTypedList(intents);
            dest.writeTypedList(selectedItems);
            dest.writeTypedList(extraItems);
            dest.writeList(touchableElements);
            dest.writeInt(resolveMedia ? 1 : 0);
            dest.writeLong(maxFileSize);
            dest.writeInt(fullScreenOnly ? 1 : 0);
        }
    }

}
