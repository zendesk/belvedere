package com.example.belvedere;

import android.content.Context;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import java.util.List;

import zendesk.belvedere.BelvedereUi;
import zendesk.belvedere.ImageStreamPopup;
import zendesk.belvedere.MediaIntent;
import zendesk.belvedere.MediaResult;
import zendesk.belvedere.PopupBackend;

public class ChatActivity extends AppCompatActivity {

    private EditText input;

    private PopupBackend popupBackend;
    private Listener listener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chat_activity);
        setSupportActionBar((Toolbar) findViewById(R.id.activity_request_toolbar));

        popupBackend = BelvedereUi.install(this);

        this.listener = new Listener();
        popupBackend.setImageStreamListener(listener);

        this.input = (EditText) findViewById(R.id.input);

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
        public void onImageSelected(List<MediaResult> mediaResults) {
            System.out.println("========= " + mediaResults);
        }

    }

    private void showImageStream() {
        BelvedereUi
                .imageStream(ChatActivity.this)
                .withCameraIntent()
                .withDocumentIntent("*/*", true)
                .showPopup(ChatActivity.this);
    }

    private void showKeyboard(EditText editText) {
        editText.requestFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(editText, InputMethodManager.SHOW_FORCED);
    }
}
