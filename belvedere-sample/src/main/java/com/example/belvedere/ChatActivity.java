package com.example.belvedere;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Interpolator;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.animation.PathInterpolatorCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

import zendesk.belvedere.BelvedereUi;
import zendesk.belvedere.ImageStream;
import zendesk.belvedere.MediaResult;

public class ChatActivity extends AppCompatActivity {

    private EditText input;

    private ImageStream imageStream;

    private Listener listener;
    private ImageStream.ScrollListener scrollListener;
    private ImageStream.SendListener sendListener = new SendListener();

    static List<MediaResult> mediaResults = new ArrayList<>();
    static Collection<MediaResult> extraResults = new LinkedHashSet<>();
    private FakeAdapter adapter = new FakeAdapter();

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
        imageStream.addSendListener(sendListener);
        input = findViewById(R.id.input);

        findViewById(R.id.send).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(imageStream.isAttachmentsPopupVisible()){
                    imageStream.dismiss();
                }
                mediaResults.clear();
                extraResults.clear();
                ((EditText)findViewById(R.id.input)).setText("");
            }
        });

        RecyclerView recyclerView = findViewById(R.id.activity_recyclerview);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
        init();
    }

    private void init() {
        if(imageStream.getKeyboardHelper().getInputTrap().hasFocus()) {
            input.requestFocus();
        }

        if(imageStream.wasOpen()) {
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
                if(!imageStream.isAttachmentsPopupVisible()) {
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
            if(!imageStream.isAttachmentsPopupVisible()) {
                showImageStream();
            }
            adapter.notifyDataSetChanged();
        }
    }

    private class ScrollListener implements ImageStream.ScrollListener {

        @Override
        public void onScroll(int height, int scrollArea, float scrollPosition) {
            final Interpolator interpolator = PathInterpolatorCompat.create(.19f,0f,.2f,1f);
            final float interpolation = interpolator.getInterpolation((scrollPosition * .30f));
            final int bottomPadding = (int) (-1f * interpolation * scrollArea);
            findViewById(R.id.activity_input).setTranslationY(bottomPadding);
            findViewById(R.id.activity_recyclerview).setTranslationY(bottomPadding);
        }
    }

    private static class SendListener implements ImageStream.SendListener {

        @Override
        public void onMediaSent(List<MediaResult> mediaResults) {
            extraResults.clear();
            extraResults.addAll(mediaResults);
            ChatActivity.mediaResults.clear();
            ChatActivity.mediaResults.addAll(mediaResults);
        }
    }

    private void showImageStream() {
        BelvedereUi.imageStream(ChatActivity.this)
                .withCameraIntent()
                .withDocumentIntent("*/*", true)
                .withSelectedItems(new ArrayList<>(mediaResults))
                .withExtraItems(new ArrayList<>(extraResults))
                .withTouchableItems(R.id.attachment, R.id.send)
                .showPopup(ChatActivity.this);
    }

    private static class FakeAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(android.R.layout.simple_list_item_1, parent, false);
            return new RecyclerView.ViewHolder(v) {};
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            if (mediaResults.isEmpty()) {
                ((TextView)holder.itemView.findViewById(android.R.id.text1)).setText("No items selected");
            } else {
                ((TextView)holder.itemView.findViewById(android.R.id.text1)).setText(mediaResults.get(position).getName());
            }
        }

        @Override
        public int getItemCount() {
            if (mediaResults.isEmpty()) {
                return 1;
            } else {
                return mediaResults.size();
            }
        }
    }
}
