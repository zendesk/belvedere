package zendesk.belvedere;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.view.ViewGroup;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static zendesk.belvedere.MediaResult.UNKNOWN_VALUE;

public class BelvedereUi {

    private final static String FRAGMENT_TAG = "BelvedereDialog";
    private final static String EXTRA_MEDIA_INTENT = "extra_intent";
    private final static String FRAGMENT_TAG_POPUP = "belvedere_image_stream";

    public static ImageStreamBuilder imageStream(Context context) {
        return new ImageStreamBuilder(context);
    }

    public static ImageStream install(AppCompatActivity activity) {
        final FragmentManager supportFragmentManager = activity.getSupportFragmentManager();
        Fragment fragment = supportFragmentManager.findFragmentByTag(FRAGMENT_TAG_POPUP);

        ImageStream popupBackend;
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
        private String maxSizeErrorMessage;

        private ImageStreamBuilder(Context context){
            this.context = context;
        }

        public ImageStreamBuilder withMediaIntents(List<MediaIntent> mediaIntents) {
            this.mediaIntents.addAll(mediaIntents);
            return this;
        }

        public ImageStreamBuilder withMediaIntents(MediaIntent... mediaIntents) {
            this.mediaIntents.addAll(Arrays.asList(mediaIntents));
            return this;
        }

        public ImageStreamBuilder withCameraIntent() {
            final MediaIntent cameraIntent = Belvedere.from(context).camera().build();
            this.mediaIntents.add(cameraIntent);
            return this;
        }

        public ImageStreamBuilder withDocumentIntent(String contentType, boolean allowMultiple) {
            final MediaIntent mediaIntent = Belvedere.from(context)
                    .document()
                    .allowMultiple(allowMultiple)
                    .contentType(contentType)
                    .build();
            this.mediaIntents.add(mediaIntent);
            return this;
        }

        public ImageStreamBuilder withSelectedItems(List<MediaResult> mediaResults) {
            this.selectedItems = new ArrayList<>(mediaResults);
            return this;
        }

        public ImageStreamBuilder withExtraItems(List<MediaResult> mediaResults) {
            this.extraItems = new ArrayList<>(mediaResults);
            return this;
        }

        public ImageStreamBuilder withTouchableItems(int... ids) {
            final List<Integer> objects = new ArrayList<>(ids.length);
            for(int id : ids) {
                objects.add(id);
            }
            this.touchableItems = objects;
            return this;
        }

        public ImageStreamBuilder withSelectedItemsUri(List<Uri> selectedItems) {
            final List<MediaResult> mediaResults = new ArrayList<>(selectedItems.size());
            for(Uri uri : selectedItems) {
                mediaResults.add(new MediaResult(null, uri, uri, null, null, UNKNOWN_VALUE, UNKNOWN_VALUE, UNKNOWN_VALUE));
            }
            this.selectedItems = mediaResults;
            return this;
        }

        public ImageStreamBuilder withMaxFileSize(long maxFileSize, String errorMessage) {
            this.maxFileSize = maxFileSize;
            this.maxSizeErrorMessage = errorMessage;
            return this;
        }

        public void showPopup(AppCompatActivity activity) {
            final ImageStream popupBackend = BelvedereUi.install(activity);
            final WeakReference<AppCompatActivity> activityReference = new WeakReference<>(activity);

            popupBackend.handlePermissions(mediaIntents, new PermissionManager.PermissionCallback() {
                @Override
                public void onPermissionsGranted(final List<MediaIntent> mediaIntents) {
                    final AppCompatActivity appCompatActivity = activityReference.get();

                    if(appCompatActivity != null) {
                        final ViewGroup decorView = (ViewGroup) appCompatActivity.getWindow().getDecorView();
                        decorView.post(new Runnable() {
                            @Override
                            public void run() {
                                final UiConfig uiConfig = new UiConfig(mediaIntents, selectedItems, extraItems, resolveMedia, touchableItems, maxFileSize, maxSizeErrorMessage);
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
                    AppCompatActivity appCompatActivity = activityReference.get();
                    if(appCompatActivity != null) {
                        Toast.makeText(appCompatActivity, "Permissions denied", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }

    @Deprecated
    public static void showDialog(FragmentManager fm, List<MediaIntent> mediaIntent) {
        if (mediaIntent == null || mediaIntent.size() == 0) {
            return;
        }

        final BelvedereDialog dialog = new BelvedereDialog();
        dialog.setArguments(getBundle(mediaIntent, new ArrayList<MediaResult>(0), new ArrayList<MediaResult>(0), true, new ArrayList<Integer>(0)));
        dialog.show(fm, FRAGMENT_TAG);
    }

    @Deprecated
    public static void showDialog(FragmentManager fm, MediaIntent... mediaIntent) {
        if (mediaIntent == null || mediaIntent.length == 0) {
            return;
        }

        showDialog(fm, Arrays.asList(mediaIntent));
    }

    private static Bundle getBundle(List<MediaIntent> mediaIntent, List<MediaResult> selectedItems, List<MediaResult> extraItems, boolean resolveMedia, List<Integer> touchableIds) {

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

        final UiConfig uiConfig = new UiConfig(intents, selected, extra, resolveMedia, touchableIds, -1L, "");
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
        private final String maxSizeErrorMessage;

        public UiConfig() {
            this.intents = new ArrayList<>();
            this.selectedItems = new ArrayList<>();
            this.extraItems = new ArrayList<>();
            this.touchableElements = new ArrayList<>();
            this.resolveMedia = true;
            this.maxFileSize = -1L;
            this.maxSizeErrorMessage = "";
        }

        public UiConfig(List<MediaIntent> intents, List<MediaResult> selectedItems,
                        List<MediaResult> extraItems, boolean resolveMedia,
                        List<Integer> touchableElements, long maxFileSize,
                        String maxSizeErrorMessage) {
            this.intents = intents;
            this.selectedItems = selectedItems;
            this.extraItems = extraItems;
            this.resolveMedia = resolveMedia;
            this.touchableElements = touchableElements;
            this.maxFileSize = maxFileSize;
            this.maxSizeErrorMessage = maxSizeErrorMessage;
        }

        protected UiConfig(Parcel in) {
            this.intents = in.createTypedArrayList(MediaIntent.CREATOR);
            this.selectedItems = in.createTypedArrayList(MediaResult.CREATOR);
            this.extraItems = in.createTypedArrayList(MediaResult.CREATOR);
            this.touchableElements = new ArrayList<>();
            in.readList(touchableElements, Integer.class.getClassLoader());
            this.resolveMedia = in.readInt() == 1;
            this.maxFileSize = in.readLong();
            this.maxSizeErrorMessage = in.readString();
        }

        public List<MediaIntent> getIntents() {
            return intents;
        }

        public List<MediaResult> getSelectedItems() {
            return selectedItems;
        }

        public List<MediaResult> getExtraItems() {
            return extraItems;
        }

        public List<Integer> getTouchableElements() {
            return touchableElements;
        }

        public long getMaxFileSize() {
            return maxFileSize;
        }

        public String getMaxSizeErrorMessage() {
            return maxSizeErrorMessage;
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
            dest.writeString(maxSizeErrorMessage);
        }
    }

}
