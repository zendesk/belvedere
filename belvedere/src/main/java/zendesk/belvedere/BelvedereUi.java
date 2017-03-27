package zendesk.belvedere;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BelvedereUi {

    private final static String FRAGMENT_TAG = "BelvedereDialog";
    private final static String EXTRA_MEDIA_INTENT = "extra_intent";


    public static ImageStreamBuilder imageStream(Context context) {
        return new ImageStreamBuilder(context);
    }

    public static KeyboardHelper install(Activity activity) {
        return KeyboardHelper.inject(activity);
    }

    public static class ImageStreamBuilder {

        private final Context context;
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

        public void show(Activity activity) {
            final Intent imageStreamIntent = getImageStreamIntent(activity, mediaIntents, selectedItems);
            activity.startActivityForResult(imageStreamIntent, IntentRegistry.PLACE_HOLDER_CODE);
        }

        public void show(Fragment fragment) {
            final Intent imageStreamIntent = getImageStreamIntent(fragment.getContext(), mediaIntents, selectedItems);
            fragment.startActivityForResult(imageStreamIntent, IntentRegistry.PLACE_HOLDER_CODE);
        }

        public ImageStreamPopup showPopup(final Activity activity, ImageStreamPopup.Listener listener) {
            final KeyboardHelper inject = KeyboardHelper.inject(activity);
            return ImageStreamPopup.show(activity, (ViewGroup) activity.getWindow().getDecorView(), inject.getKeyboardHeight(), listener, new UiConfig(mediaIntents, selectedItems));
        }

    }

    @Deprecated
    public static void showDialog(FragmentManager fm, List<MediaIntent> mediaIntent) {
        if (mediaIntent == null || mediaIntent.size() == 0) {
            return;
        }

        final BelvedereDialog dialog = new BelvedereDialog();
        dialog.setArguments(getBundle(mediaIntent, new ArrayList<MediaResult>()));
        dialog.show(fm.beginTransaction(), FRAGMENT_TAG);
    }

    @Deprecated
    public static void showDialog(FragmentManager fm, MediaIntent... mediaIntent) {
        if (mediaIntent == null || mediaIntent.length == 0) {
            return;
        }

        showDialog(fm, Arrays.asList(mediaIntent));
    }

    private static Intent getImageStreamIntent(Context context, List<MediaIntent> mediaIntents, List<MediaResult> selectedItems) {
        final Intent intent = new Intent(context, ImageStream.class);
        intent.putExtras(getBundle(mediaIntents, selectedItems));
        return intent;
    }

    private static Bundle getBundle(List<MediaIntent> mediaIntent, List<MediaResult> selectedItems) {

        final List<MediaIntent> intents = new ArrayList<>();
        final List<MediaResult> selected = new ArrayList<>();

        if(mediaIntent != null) {
            intents.addAll(mediaIntent);
        }

        if(selectedItems != null) {
            selected.addAll(selectedItems);
        }

        final UiConfig uiConfig = new UiConfig(intents, selected);
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

        public UiConfig() {
            this.intents = new ArrayList<>();
            this.selectedItems = new ArrayList<>();
        }

        public UiConfig(List<MediaIntent> intents, List<MediaResult> selectedItems) {
            this.intents = intents;
            this.selectedItems = selectedItems;
        }


        protected UiConfig(Parcel in) {
            intents = in.createTypedArrayList(MediaIntent.CREATOR);
            selectedItems = in.createTypedArrayList(MediaResult.CREATOR);
        }

        public List<MediaIntent> getIntents() {
            return intents;
        }

        public List<MediaResult> getSelectedItems() {
            return selectedItems;
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
        }
    }

}
