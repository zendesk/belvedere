package zendesk.belvedere;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BelvedereUi {

    private final static String FRAGMENT_TAG = "BelvedereDialog";
    private final static String EXTRA_MEDIA_INTENT = "extra_intent";

    @Deprecated
    public static void showDialog(FragmentManager fm, List<MediaIntent> mediaIntent) {
        if (mediaIntent == null || mediaIntent.size() == 0) {
            return;
        }

        final BelvedereDialog dialog = new BelvedereDialog();
        dialog.setArguments(getBundle(mediaIntent));
        dialog.show(fm.beginTransaction(), FRAGMENT_TAG);
    }

    @Deprecated
    public static void showDialog(FragmentManager fm, MediaIntent... mediaIntent) {
        if (mediaIntent == null || mediaIntent.length == 0) {
            return;
        }

        showDialog(fm, Arrays.asList(mediaIntent));
    }

    public static void startImageStream(Activity activity, List<MediaIntent> mediaIntent) {
        if (mediaIntent == null) {
            return;
        }

        final Intent intent = new Intent(activity, ImageStream.class);
        intent.putExtras(getBundle(mediaIntent));
        activity.startActivityForResult(intent, IntentRegistry.PLACE_HOLDER_CODE);
    }

    public static void startImageStream(Activity activity, MediaIntent... mediaIntent) {
        if (mediaIntent == null || mediaIntent.length == 0) {
            return;
        }

        startImageStream(activity, Arrays.asList(mediaIntent));
    }

    public static void startImageStream(Fragment fragment, List<MediaIntent> mediaIntent) {
        if (mediaIntent == null || mediaIntent.size() == 0) {
            return;
        }

        final Intent intent = new Intent(fragment.getContext(), ImageStream.class);
        intent.putExtras(getBundle(mediaIntent));
        fragment.startActivityForResult(intent, IntentRegistry.PLACE_HOLDER_CODE);
    }

    public static void startImageStream(Fragment fragment, MediaIntent... mediaIntent) {
        if (mediaIntent == null || mediaIntent.length == 0) {
            return;
        }

        startImageStream(fragment, mediaIntent);
    }

    private static Bundle getBundle(List<MediaIntent> mediaIntent) {
        final ArrayList<MediaIntent> filteredList = new ArrayList<>();
        if (mediaIntent != null && mediaIntent.size() != 0) {
            for (MediaIntent intent : mediaIntent) {
                if (intent.isAvailable()) {
                    filteredList.add(intent);
                }
            }
        }

        final Bundle bundle = new Bundle();
        bundle.putParcelableArrayList(EXTRA_MEDIA_INTENT, filteredList);

        return bundle;
    }

    static List<MediaIntent> getMediaIntents(Bundle bundle) {
        List<MediaIntent> intents = bundle.getParcelableArrayList(EXTRA_MEDIA_INTENT);

        if (intents == null || intents.size() == 0) {
            return new ArrayList<>();
        }

        return intents;

    }

}
