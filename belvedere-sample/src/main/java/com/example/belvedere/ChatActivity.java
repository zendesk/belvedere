package com.example.belvedere;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import java.util.ArrayList;
import java.util.List;

import zendesk.belvedere.BelvedereUi;
import zendesk.belvedere.ImageStreamPopup;
import zendesk.belvedere.MediaResult;
import zendesk.belvedere.PopupBackend;

public class ChatActivity extends AppCompatActivity {

    private EditText input;

    private PopupBackend popupBackend;

    private Listener listener;

    static List<MediaResult> mediaResults = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chat_activity);
        setSupportActionBar((Toolbar) findViewById(R.id.activity_request_toolbar));

        popupBackend = BelvedereUi.install(this);

        this.listener = new Listener();
        popupBackend.setImageStreamListener(listener);

        this.input = (EditText) findViewById(R.id.input);

        findViewById(R.id.send).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mediaResults.clear();
                ((Button)findViewById(R.id.attachment)).setText(mediaResults.size()+"");
            }
        });

        init();
    }

    private void init() {
        if(popupBackend.getKeyboardHelper().inputTrap.hasFocus()) {
            input.requestFocus();
        }

        if(popupBackend.wasOpen()) {
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
                showImageStream();
            }
        });
    }

    class Listener implements ImageStreamPopup.Listener {

        @Override
        public void onDismissed() {
            input.requestFocus();
        }

        @Override
        public void onImageSelected(List<MediaResult> r, boolean replace) {
            if(!popupBackend.isAttachmentsPopupVisible2()) {
                showImageStream();
            }

            if(replace) {
                mediaResults.clear();
            }
            mediaResults.addAll(new ArrayList<>(r));
            ((Button)findViewById(R.id.attachment)).setText(mediaResults.size()+"");
        }

    }

    private void showImageStream() {
        BelvedereUi
                .imageStream(ChatActivity.this)
                .withCameraIntent()
                .withDocumentIntent("*/*", true)
                .withSelectedItems(new ArrayList<>(mediaResults))
                .showPopup(ChatActivity.this);
    }
}
