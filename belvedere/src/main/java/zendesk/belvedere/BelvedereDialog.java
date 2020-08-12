package zendesk.belvedere;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDialogFragment;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;

import java.util.ArrayList;
import java.util.List;

import zendesk.belvedere.ui.R;

/**
 * This is a {@link DialogFragment} that allows the user to select an image source.
 * <p>
 * <p>
 * Based on the available permissions, this dialog allows the user to select images from a gallery or
 * from a camera app.
 * </p>
 */
public class BelvedereDialog extends AppCompatDialogFragment {

    private static final int REQUEST_ID = 1212;

    private final static String STATE_WAITING_FOR_PERMISSION = "waiting_for_permission";

    private ListView listView;
    private MediaIntent waitingForPermission;
    private List<MediaIntent> mediaIntents;
    private PermissionStorage preferences;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.belvedere_dialog, container, false);
        listView = view.findViewById(R.id.belvedere_dialog_listview);
        return view;
    }

    @Override
    public void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferences = new PermissionStorage(requireContext());
        if (savedInstanceState != null) {
            waitingForPermission = savedInstanceState.getParcelable(STATE_WAITING_FOR_PERMISSION);
        }
        setStyle(STYLE_NO_TITLE, getTheme());
    }

    @Override
    public void onStart() {
        super.onStart();
        mediaIntents = getMediaIntents();
        fillList(mediaIntents);
    }

    private void askForPermission(MediaIntent mediaIntent) {
        this.waitingForPermission = mediaIntent;
        requestPermissions(new String[]{mediaIntent.getPermission()}, REQUEST_ID);
    }

    @Override
    public void onRequestPermissionsResult(final int requestCode, @NonNull final String[] permissions, @NonNull final int[] grantResults) {

        if (requestCode == REQUEST_ID && waitingForPermission != null && !TextUtils.isEmpty(waitingForPermission.getPermission())) {
            if (permissions.length > 0 && permissions[0].equals(waitingForPermission.getPermission())) {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    if (getParentFragment() != null) {
                        waitingForPermission.open(getParentFragment());
                    } else if (getActivity() != null) {
                        waitingForPermission.open(getActivity());
                    }

                    dismissAllowingStateLoss();

                } else if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_DENIED) {

                    boolean showRationale = shouldShowRequestPermissionRationale(waitingForPermission.getPermission());

                    if (!showRationale) {
                        preferences.neverEverAskForThatPermissionAgain(waitingForPermission.getPermission());
                        mediaIntents = getMediaIntents();
                        fillList(mediaIntents);
                    }

                }

                waitingForPermission = null;
            }

        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull final Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(STATE_WAITING_FOR_PERMISSION, waitingForPermission);
    }

    private void fillList(final List<MediaIntent> intents) {

        if (getParentFragment() != null) {

            final Fragment parentFragment = getParentFragment();
            fillListView(new StartActivity() {
                @Override
                public void startActivity(final MediaIntent mediaIntent) {
                    mediaIntent.open(parentFragment);
                }

                @Override
                public Context getContext() {
                    return parentFragment.getContext();
                }
            }, intents);

        } else if (getActivity() != null) {

            final FragmentActivity activity = getActivity();
            fillListView(new StartActivity() {
                @Override
                public void startActivity(final MediaIntent mediaIntent) {
                    mediaIntent.open(activity);
                }

                @Override
                public Context getContext() {
                    return activity;
                }
            }, intents);

        } else {
            if (isAdded()) {
                dismiss();
            }
        }
    }

    private void fillListView(final StartActivity activity, final List<MediaIntent> intents) {
        listView.setAdapter(new Adapter(activity.getContext(), R.layout.belvedere_dialog_row, intents));
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(@NonNull final AdapterView<?> parent, @NonNull final View view, final int position, final long id) {
                if (view.getTag() instanceof MediaIntent) {
                    openBelvedereIntent((MediaIntent) view.getTag(), activity);
                }
            }
        });

        if (intents.size() == 0) {
            dismissAllowingStateLoss();

        } else if (intents.size() == 1) {
            openBelvedereIntent(intents.get(0), activity);
        }
    }

    private void openBelvedereIntent(MediaIntent belvedereIntent, StartActivity startActivity) {
        if (TextUtils.isEmpty(belvedereIntent.getPermission())) {
            startActivity.startActivity(belvedereIntent);
            dismiss();
        } else {
            askForPermission(belvedereIntent);
        }
    }

    private List<MediaIntent> getMediaIntents() {
        List<MediaIntent> intents = BelvedereUi.getUiConfig(requireArguments()).getIntents();
        List<MediaIntent> filter = new ArrayList<>();
        for (MediaIntent belvedereIntent : intents) {
            if (TextUtils.isEmpty(belvedereIntent.getPermission())
                    || !preferences.shouldINeverEverAskForThatPermissionAgain(belvedereIntent.getPermission())
                    || belvedereIntent.isAvailable()) {
                filter.add(belvedereIntent);
            }
        }

        return filter;
    }

    private static class Adapter extends ArrayAdapter<MediaIntent> {

        private Context context;

        Adapter(Context context, int resource, List<MediaIntent> objects) {
            super(context, resource, objects);
            this.context = context;
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            View row = convertView;
            if (convertView == null) {
                row = LayoutInflater.from(context).inflate(R.layout.belvedere_dialog_row, parent, false);
            }

            final MediaIntent intent = getItem(position);
            final AttachmentSource item = AttachmentSource.from(intent, context);
            ((ImageView) row.findViewById(R.id.belvedere_dialog_row_image)).setImageDrawable(ContextCompat.getDrawable(context, item.getDrawable()));
            ((TextView) row.findViewById(R.id.belvedere_dialog_row_text)).setText(item.getText());
            row.setTag(intent);

            return row;
        }
    }

    private static class AttachmentSource {

        private final int drawable;
        private final String text;

        public static AttachmentSource from(MediaIntent belvedereIntent, Context context) {
            if (belvedereIntent.getTarget() == MediaIntent.TARGET_CAMERA) {
                return new AttachmentSource(R.drawable.belvedere_ic_camera, context.getString(R.string.belvedere_dialog_camera));
            } else if (belvedereIntent.getTarget() == MediaIntent.TARGET_DOCUMENT) {
                return new AttachmentSource(R.drawable.belvedere_ic_image, context.getString(R.string.belvedere_dialog_gallery));
            } else {
                return new AttachmentSource(-1, "");
            }
        }

        private AttachmentSource(int drawable, String text) {
            this.drawable = drawable;
            this.text = text;
        }

        public int getDrawable() {
            return drawable;
        }

        public String getText() {
            return text;
        }
    }

    private interface StartActivity {
        void startActivity(MediaIntent mediaIntent);

        Context getContext();
    }
}
