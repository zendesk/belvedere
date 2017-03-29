package zendesk.belvedere;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
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

public class BelvedereUi {

    private final static String FRAGMENT_TAG = "BelvedereDialog";
    private final static String EXTRA_MEDIA_INTENT = "extra_intent";

    private final static String FRAGMENT_TAG_POPUP = "bla";

    public static ImageStreamBuilder imageStream(Context context) {
        return new ImageStreamBuilder(context);
    }

    public static PopupBackend install(AppCompatActivity activity) {
        final FragmentManager supportFragmentManager = activity.getSupportFragmentManager();
        Fragment fragment = supportFragmentManager.findFragmentByTag(FRAGMENT_TAG_POPUP);

        PopupBackend popupBackend;
        if(fragment instanceof PopupBackend) {
            popupBackend = (PopupBackend) fragment;
        } else {
            popupBackend = new PopupBackend();
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

        private ImageStreamBuilder(Context context){
            this.context = context;
        }

        public ImageStreamBuilder withMediaIntents(List<MediaIntent> mediaIntents) {
            this.mediaIntents = mediaIntents;
            return this;
        }

        public ImageStreamBuilder withMediaIntents(MediaIntent... mediaIntents) {
            this.mediaIntents = Arrays.asList(mediaIntents);
            return this;
        }

        public ImageStreamBuilder withCameraIntent() {
            this.mediaIntents.add(Belvedere.from(context).camera().build());
            return this;
        }

        public ImageStreamBuilder withDocumentIntent(String contentType, boolean allowMultiple) {
            this.mediaIntents.add(Belvedere.from(context).document().allowMultiple(allowMultiple).contentType(contentType).build());
            return this;
        }

        public ImageStreamBuilder withSelectedItems(List<MediaResult> mediaResults) {
            this.selectedItems = mediaResults;
            return this;
        }

        public ImageStreamBuilder resolveMedia(boolean enabled) {
            this.resolveMedia = enabled;
            return this;
        }

        public void show(Activity activity) {
//            final Intent imageStreamIntent = getImageStreamIntent(activity, mediaIntents, selectedItems, resolveMedia);
//            activity.startActivityForResult(imageStreamIntent, IntentRegistry.PLACE_HOLDER_CODE);
        }

        public void show(Fragment fragment) {
//            final Intent imageStreamIntent = getImageStreamIntent(fragment.getContext(), mediaIntents, selectedItems, resolveMedia);
//            fragment.startActivityForResult(imageStreamIntent, IntentRegistry.PLACE_HOLDER_CODE);
        }

        public void showPopup(AppCompatActivity activity) {
            final PopupBackend popupBackend = BelvedereUi.install(activity);

            final WeakReference<AppCompatActivity> activityReference = new WeakReference<>(activity);

            popupBackend.handlePermissionStuffForStream(mediaIntents, new PopupBackend.PermissionCallback() {
                @Override
                public void ok(final List<MediaIntent> mediaIntents) {
                    final AppCompatActivity appCompatActivity = activityReference.get();

                    if(appCompatActivity != null) {
                        final ViewGroup decorView = (ViewGroup) appCompatActivity.getWindow().getDecorView();
                        decorView.post(new Runnable() {
                            @Override
                            public void run() {
                                final ImageStreamPopup show = ImageStreamPopup.show(
                                        appCompatActivity, decorView,
                                        popupBackend,
                                        new UiConfig(mediaIntents, selectedItems, resolveMedia));
                                popupBackend.setImageStreamPopup(show);
                            }
                        });
                    }
                }

                @Override
                public void nope() {
                    AppCompatActivity appCompatActivity = activityReference.get();
                    if(appCompatActivity != null) {
                        Toast.makeText(appCompatActivity, "nope", Toast.LENGTH_SHORT).show();
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
        dialog.setArguments(getBundle(mediaIntent, new ArrayList<MediaResult>(), true));
        dialog.show(fm.beginTransaction(), FRAGMENT_TAG);
    }

    @Deprecated
    public static void showDialog(FragmentManager fm, MediaIntent... mediaIntent) {
        if (mediaIntent == null || mediaIntent.length == 0) {
            return;
        }

        showDialog(fm, Arrays.asList(mediaIntent));
    }


    private static Intent getImageStreamIntent(Context context, List<MediaIntent> mediaIntents, List<MediaResult> selectedItems, boolean resolveMedia) {
        //final Intent intent = new Intent(context, ImageStream.class);
        //intent.putExtras(getBundle(mediaIntents, selectedItems, resolveMedia));
//        return intent;
        return null;
    }

    private static Bundle getBundle(List<MediaIntent> mediaIntent, List<MediaResult> selectedItems, boolean resolveMedia) {

        final List<MediaIntent> intents = new ArrayList<>();
        final List<MediaResult> selected = new ArrayList<>();

        if(mediaIntent != null) {
            intents.addAll(mediaIntent);
        }

        if(selectedItems != null) {
            selected.addAll(selectedItems);
        }

        final UiConfig uiConfig = new UiConfig(intents, selected, resolveMedia);
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
        private final boolean resolveMedia;

        public UiConfig() {
            this.intents = new ArrayList<>();
            this.selectedItems = new ArrayList<>();
            this.resolveMedia = true;
        }

        public UiConfig(List<MediaIntent> intents, List<MediaResult> selectedItems, boolean resolveMedia) {
            this.intents = intents;
            this.selectedItems = selectedItems;
            this.resolveMedia = resolveMedia;
        }

        protected UiConfig(Parcel in) {
            intents = in.createTypedArrayList(MediaIntent.CREATOR);
            selectedItems = in.createTypedArrayList(MediaResult.CREATOR);
            resolveMedia = in.readInt() == 1;
        }

        public List<MediaIntent> getIntents() {
            return intents;
        }

        public List<MediaResult> getSelectedItems() {
            return selectedItems;
        }

        public boolean shouldResolveMedia() {
            return resolveMedia;
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
            dest.writeInt(resolveMedia ? 1 : 0);
        }
    }

}
