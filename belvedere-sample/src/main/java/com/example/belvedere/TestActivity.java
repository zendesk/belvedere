package com.example.belvedere;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import java.util.List;

import zendesk.belvedere.Belvedere;
import zendesk.belvedere.Callback;
import zendesk.belvedere.ImageStream;
import zendesk.belvedere.MediaResult;


public class TestActivity extends AppCompatActivity {

    private ImageStream imageStream;
    private Button selectAttachmentFromCamera;
    private Button selectAttachmentFromDocuments;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // ...

        MediaResult mediaResult = Belvedere.from(this).getFile("direName", "test.jpg");

        Belvedere.from(this).getShareIntent(mediaResult.getUri(), mediaResult.getMimeType());
        Belvedere.from(this).getViewIntent(mediaResult.getUri(), mediaResult.getMimeType());

        selectAttachmentFromCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Belvedere.from(TestActivity.this)
                        .camera()
                        .open(TestActivity.this);
            }
        });

        selectAttachmentFromDocuments.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Belvedere.from(TestActivity.this)
                        .document()
                        .open(TestActivity.this);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Belvedere.from(this).getFilesFromActivityOnResult(requestCode, resultCode, data, new Callback<List<MediaResult>>() {
            @Override
            public void success(List<MediaResult> result) {
                // Handle Selected files
            }
        });
    }
}
