package com.example.belvedere;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import zendesk.belvedere.BelvedereUi;
import zendesk.belvedere.ImageStreamPopup;
import zendesk.belvedere.KeyboardHelper;

public class ChatActivity extends AppCompatActivity {

    private EditText input, inputTrap;

    private KeyboardHelper keyboardHelper;
    private ImageStreamPopup attachmentPicker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chat_activity);
        setSupportActionBar((Toolbar) findViewById(R.id.activity_request_toolbar));
        this.keyboardHelper = BelvedereUi.install(this);

        this.inputTrap = (EditText) findViewById(R.id.fake_edit_text);
        this.input = (EditText) findViewById(R.id.input);

        init();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(attachmentPicker != null) {
            attachmentPicker.dismiss();
            attachmentPicker = null;
        }
    }

    private void init() {
        if(inputTrap.hasFocus()) {
            input.requestFocus();
        }

        findViewById(R.id.attachment).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(keyboardHelper.isKeyboardVisible()) {
                    inputTrap.requestFocus();
                    showImageStream();

                } else {
                    keyboardHelper.setListener(new KeyboardHelper.Listener() {
                        @Override
                        public void onKeyboardVisible() {
                            keyboardHelper.setListener(null);
                            showImageStream();
                        }
                    });
                    showKeyboard(inputTrap);
                }
            }
        });
    }

    class Listener implements ImageStreamPopup.Listener {

        @Override
        public void onDismissed() {
            input.requestFocus();
        }

    }

    private ImageStreamPopup showImageStream() {
        return BelvedereUi
                .imageStream(ChatActivity.this)
                .withCameraIntent()
                .withDocumentIntent("*/*", true)
                .showPopup(ChatActivity.this, new Listener());
    }

    private void showKeyboard(EditText editText) {
        editText.requestFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(editText, InputMethodManager.SHOW_FORCED);
    }
}
