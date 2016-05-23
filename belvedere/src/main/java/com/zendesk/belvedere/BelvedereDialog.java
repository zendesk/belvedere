package com.zendesk.belvedere;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatDialogFragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * This is a {@link DialogFragment} that allows the user to select an image source.
 *
 * <p>
 *     Based on the available permissions, this dialog allows the user to select images from a gallery or
 *     from a camera app.
 * </p>
 */
public class BelvedereDialog extends AppCompatDialogFragment {

    private static final int REQUEST_ID = 12;

    private final static String LOG_TAG = "BelvedereDialog";
    private final static String FRAGMENT_TAG = "BelvedereDialog";
    private final static String EXTRA_INTENT = "extra_intent";

    private ListView listView;
    private BelvedereIntent waitingForPermission;
    private List<BelvedereIntent> belvedereIntents;
    private BelvedereSharedPreferences preferences;

    /**
     * Show a {@link BelvedereDialog} and render the specified {@link BelvedereIntent}.
     *
     * @param fm A valid {@link FragmentManager}
     * @param belvedereIntent A list of {@link BelvedereIntent} to display
     */
    public static void showDialog(FragmentManager fm, List<BelvedereIntent> belvedereIntent){

        if(belvedereIntent == null || belvedereIntent.size() == 0){
            return;
        }

        final BelvedereDialog attachmentSourceSelectorDialog = new BelvedereDialog();
        final Bundle bundle = new Bundle();

        bundle.putParcelableArrayList(EXTRA_INTENT, new ArrayList<Parcelable>(belvedereIntent));
        attachmentSourceSelectorDialog.setArguments(bundle);

        attachmentSourceSelectorDialog.show(fm.beginTransaction(), FRAGMENT_TAG);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.belvedere_dialog, container, false);
        listView = (ListView) view.findViewById(R.id.belvedere_dialog_listview);
        setRetainInstance(true);
        return view;
    }

    @Override
    public void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferences = new BelvedereSharedPreferences(getContext());
        setStyle(STYLE_NO_TITLE, getTheme());
    }

    @Override
    public void onStart() {
        super.onStart();
        belvedereIntents = getBelvedereIntents();
        fillList(belvedereIntents);
    }

    private void askForPermission(BelvedereIntent belvedereIntent) {
        this.waitingForPermission = belvedereIntent;
        requestPermissions(new String[]{belvedereIntent.getPermission()}, REQUEST_ID);
    }

    @Override
    public void onRequestPermissionsResult(final int requestCode, @NonNull final String[] permissions, @NonNull final int[] grantResults) {

        if (requestCode == REQUEST_ID && waitingForPermission != null && !TextUtils.isEmpty(waitingForPermission.getPermission())) {
            if(permissions.length > 0 && permissions[0].equals(waitingForPermission.getPermission())) {
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    if(getActivity() != null){
                        waitingForPermission.open(getActivity());
                    } else if(getParentFragment() != null) {
                        waitingForPermission.open(getParentFragment());
                    }

                    dismissAllowingStateLoss();

                } else if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_DENIED) {

                    boolean showRationale = shouldShowRequestPermissionRationale(waitingForPermission.getPermission());

                    if(!showRationale){
                        preferences.neverEverAskForThatPermissionAgain(waitingForPermission.getPermission());
                        belvedereIntents = getBelvedereIntents();
                        fillList(belvedereIntents);
                    }
                }

                waitingForPermission = null;
            }

        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    public void onDestroyView() {
        if (getDialog() != null && getRetainInstance()){
            getDialog().setDismissMessage(null);
        }
        super.onDestroyView();
    }

    private List<BelvedereIntent> getBelvedereIntents(){
        List<BelvedereIntent> intents = getArguments().getParcelableArrayList(EXTRA_INTENT);

        if(intents == null || intents.size() == 0){
            return new ArrayList<>();
        }

        List<BelvedereIntent> filter = new ArrayList<>();
        for(BelvedereIntent belvedereIntent : intents) {
            if (TextUtils.isEmpty(belvedereIntent.getPermission())
                    || !preferences.shouldINeverEverAskForThatPermissionAgain(belvedereIntent.getPermission())) {
                filter.add(belvedereIntent);
            }
        }

        return filter;
    }

    private void fillList(final List<BelvedereIntent> intents) {

        if(getParentFragment() != null){

            final Fragment parentFragment = getParentFragment();
            fillListView(new StartActivity() {
                @Override
                public void startActivity(final BelvedereIntent belvedereIntent) {
                    belvedereIntent.open(parentFragment);
                }

                @Override
                public Context getContext() {
                    return parentFragment.getContext();
                }
            }, intents);

        } else if(getActivity() != null){

            final FragmentActivity activity = getActivity();
            fillListView(new StartActivity() {
                @Override
                public void startActivity(final BelvedereIntent belvedereIntent) {
                    belvedereIntent.open(activity);
                }

                @Override
                public Context getContext() {
                    return activity;
                }
            }, intents);

        } else {
            Log.w(LOG_TAG, "Not able to find a valid context for starting an BelvedereIntent");
            if(getFragmentManager() != null){
                dismiss();
            }
        }
    }

    private void fillListView(final StartActivity activity, final List<BelvedereIntent> intents){
        listView.setAdapter(new Adapter(activity.getContext(), R.layout.belvedere_dialog_row, intents));
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(@NonNull final AdapterView<?> parent, @NonNull final View view, final int position, final long id) {
                if (view.getTag() instanceof BelvedereIntent) {
                    final BelvedereIntent intent = (BelvedereIntent) view.getTag();

                    if(TextUtils.isEmpty(intent.getPermission())){
                        activity.startActivity(((BelvedereIntent) view.getTag()));
                        dismiss();
                    } else {
                        askForPermission(intent);
                    }
                }
            }
        });
    }

    private class Adapter extends ArrayAdapter<BelvedereIntent>{

        private Context context;

        Adapter(Context context, int resource, List<BelvedereIntent> objects) {
            super(context, resource, objects);
            this.context = context;
        }

        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            View row = convertView;
            if(convertView == null){
                row = LayoutInflater.from(context).inflate(R.layout.belvedere_dialog_row, parent, false);
            }

            final BelvedereIntent intent = getItem(position);
            final AttachmentSource item = AttachmentSource.from(intent, context);
            ((ImageView) row.findViewById(R.id.belvedere_dialog_row_image)).setImageDrawable(ContextCompat.getDrawable(context, item.getDrawable()));
            ((TextView) row.findViewById(R.id.belvedere_dialog_row_text)).setText(item.getText());
            row.setTag(intent);

            return row;
        }
    }

    private static class AttachmentSource{

        private final int drawable;
        private final String text;

        public static AttachmentSource from(BelvedereIntent belvedereIntent, Context context){
            switch (belvedereIntent.getSource()){
                case Camera:
                    return new AttachmentSource(R.drawable.ic_camera, context.getString(R.string.belvedere_dialog_camera));
                case Gallery:
                    return new AttachmentSource(R.drawable.ic_image, context.getString(R.string.belvedere_dialog_gallery));
                default:
                    return new AttachmentSource(-1, context.getString(R.string.belvedere_dialog_unknown));
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
        void startActivity(BelvedereIntent belvedereIntent);
        Context getContext();
    }
}
