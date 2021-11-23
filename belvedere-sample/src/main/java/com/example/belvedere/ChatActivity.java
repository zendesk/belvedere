package com.example.belvedere;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Interpolator;
import android.widget.EditText;
import android.widget.TextView;

import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.animation.PathInterpolatorCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

import zendesk.belvedere.BelvederePermissionCallback;
import zendesk.belvedere.BelvedereUi;
import zendesk.belvedere.ImageStream;
import zendesk.belvedere.MediaIntent;
import zendesk.belvedere.MediaResult;

public class ChatActivity extends AppCompatActivity {

    private EditText input;

    private ImageStream imageStream;

    private Listener listener;
    private ImageStream.ScrollListener scrollListener;

    static List<MediaResult> mediaResults = new ArrayList<>();
    static Collection<MediaResult> extraResults = new LinkedHashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chat_activity);
        setSupportActionBar((Toolbar) findViewById(R.id.activity_request_toolbar));
        imageStream = BelvedereUi.install(this);
        listener = new Listener();
        scrollListener = new ScrollListener();
        imageStream.addListener(listener);
        imageStream.addScrollListener(scrollListener);
        input = findViewById(R.id.input);

        findViewById(R.id.send).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (imageStream.isAttachmentsPopupVisible()) {
                    imageStream.dismiss();
                }
                mediaResults.clear();
                extraResults.clear();
                ((EditText) findViewById(R.id.input)).setText("");
            }
        });

        RecyclerView recyclerView = findViewById(R.id.activity_recyclerview);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(new FakeAdapter());
        init();
    }

    private void init() {
        if (imageStream.getKeyboardHelper().getInputTrap().hasFocus()) {
            input.requestFocus();
        }

        if (imageStream.wasOpen()) {
            input.post(new Runnable() {
                @Override
                public void run() {
                    showImageStream();
                }
            });
        }

        findViewById(R.id.attachment).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!imageStream.isAttachmentsPopupVisible()) {
                    showImageStream();
                } else {
                    imageStream.dismiss();
                }
            }
        });
    }

    private class Listener implements ImageStream.Listener {

        @Override
        public void onDismissed() {
            input.requestFocus();
        }

        @Override
        public void onVisible() {
            // Intentionally empty.
        }

        @Override
        public void onMediaSelected(List<MediaResult> a) {
            mediaResults.addAll(0, new ArrayList<>(a));
            extraResults.addAll(new ArrayList<>(a));
            refreshUi();
        }

        @Override
        public void onMediaDeselected(List<MediaResult> a) {
            mediaResults.removeAll(new ArrayList<>(a));
            refreshUi();
        }

        private void refreshUi() {
            if (!imageStream.isAttachmentsPopupVisible()) {
                showImageStream();
            }
        }
    }

    private class ScrollListener implements ImageStream.ScrollListener {

        @Override
        public void onScroll(int height, int scrollArea, float scrollPosition) {
            final Interpolator interpolator = PathInterpolatorCompat.create(.19f, 0f, .2f, 1f);
            final float interpolation = interpolator.getInterpolation((scrollPosition * .30f));
            final int bottomPadding = (int) (-1f * interpolation * scrollArea);
            findViewById(R.id.activity_input).setTranslationY(bottomPadding);
            findViewById(R.id.activity_recyclerview).setTranslationY(bottomPadding);
        }
    }

    private void showImageStream() {
        BelvedereUi.imageStream(ChatActivity.this)
                .withCameraIntent()
                .withDocumentIntent("*/*", true)
                .withSelectedItems(new ArrayList<>(mediaResults))
                .withExtraItems(new ArrayList<>(extraResults))
                .withTouchableItems(R.id.attachment, R.id.send)
                .withBelvederePermissionCallback(new BelvederePermissionCallback() {
                    @Override
                    public void onPermissionsGranted(List<MediaIntent> mediaIntents) {

                    }

                    @Override
                    public void onPermissionsDenied(boolean isMoreThanOnce) {
                        if (isMoreThanOnce) {
                            Snackbar snackbar = Snackbar
                                    .make(findViewById(android.R.id.content),
                                            "To send attachments, you must grant permissions in your settings",
                                            Snackbar.LENGTH_LONG);
                            snackbar.setAction("Settings", new OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    Intent intent = new Intent();
                                    intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                    intent.setData(Uri.fromParts("package", ChatActivity.this.getPackageName(), null));
                                    ChatActivity.this.startActivity(intent);
                                }
                            });
                            snackbar.show();
                        } else {
                            Toast.makeText(ChatActivity.this, R.string.belvedere_permissions_denied, Toast.LENGTH_SHORT)
                                    .show();
                        }
                    }
                })
                .showPopup(ChatActivity.this);
    }

    class FakeAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(android.R.layout.simple_list_item_1, parent, false);
            return new RecyclerView.ViewHolder(v) {
            };
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            ((TextView) holder.itemView.findViewById(android.R.id.text1)).setText("Belvedere Demo");
        }

        @Override
        public int getItemCount() {
            return 1;
        }
    }

}
