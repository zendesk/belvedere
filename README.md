<p align="center">

# Belvedere
<p align="left">
<a href="https://travis-ci.org/zendesk/Suas-Android"><img src="https://travis-ci.org/zendesk/belvedere.svg?branch=master" alt="Build Status" /></a>
<a href="https://raw.githubusercontent.com/zendesk/Suas-Android/master/LICENSE"><img src="https://img.shields.io/badge/License-Apache%202.0-blue.svg" alt="License" /></a>
<img src="https://img.shields.io/maven-central/v/com.zendesk.belvedere2/suas.svg" alt="Belvedere version" />
</p>

A file picker for Android.
<br />

<p align="center">
<img width="300" src="https://github.com/zendesk/belvedere/raw/schlan/javadoc/media/belvedere_stream_demo.gif"/>
</p>

### Overview
Belvedere gives you the power to easily integrate file selection from third party apps and the camera without the need to take care of permissions, ContentProvider, Intent permissions, and so on.

### Download

Add Belvedere as a dependency:

```
compile ‘com.zendesk.belvedere2:belvedere:2.0.0’
```

### How to use Belvedere

#### ImageStream

A simple implementation of the ImageStream looks like this:

```java
public class TestActivity extends AppCompatActivity implements ImageStream.Listener {

    private ImageStream imageStream;
    private Button selectAttachment;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // ...

        imageStream = BelvedereUi.install(this);
        imageStream.addListener(this);

        selectAttachment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                BelvedereUi.imageStream(getApplicationContext())
                        .withCameraIntent()
                        .withDocumentIntent("*/*", true)
                        .showPopup(TestActivity.this);
            }
        });
    }

    @Override
    public void onDismissed() {
        // Image Stream was dismissed
    }

    @Override
    public void onVisible() {
        // Image Stream was shown
    }

    @Override
    public void onMediaSelected(List<MediaResult> mediaResults) {
        // The user selected attachments
    }

    @Override
    public void onMediaDeselected(List<MediaResult> mediaResults) {
        // The user deselected attachments
    }
}
```

#### Dialog (from 1.x)


```java
public class TestActivity extends AppCompatActivity {

    private ImageStream imageStream;
    private Button selectAttachment;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // ...

        selectAttachment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Belvedere belvedere = Belvedere.from(this);
                MediaIntent document = belvedere.document().build();
                MediaIntent camera = belvedere.camera().build();

                BelvedereUi.showDialog(getSupportFragmentManager(), document, camera);
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
```

#### API only

Select an image from the camera.

```java
public class TestActivity extends AppCompatActivity {

    private ImageStream imageStream;
    private Button selectAttachmentFromCamera;
    private Button selectAttachmentFromDocuments;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // ...

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
```


#### Place a file into Belvedere’s internal storage
Moreover, it’s possible to put your own data into Belvedere’s cache. To get access to an internal file, call:

```java
MediaResult mediaResult = Belvedere.from(this).getFile("dire_name", "file_name.jpg");
```
Again, you’ll get a file object and a `Uri`. For example, you can use the file to open a `FileOutputStream`.

#### Open or share an internal file
Files that are available through Belvedere could be opened or shared with other apps. To do that, use the `Uri` you get from a `BelvedereResult`.

Use the first code snippet to open a file and the second one to share a file:

```java
Intent viewIntent = Belvedere.from(this).getViewIntent(mediaResult.getUri(), mediaResult.getMimeType());
startActivity(viewIntent);
```

```java
Intent shareIntent = Belvedere.from(this).getShareIntent(mediaResult.getUri(), mediaResult.getMimeType());
startActivity(shareIntent);
```


### Contributing

Bug reports, feature requests and contributions are very welcome. Please follow these steps to contribute:
 - Submit a Pull Request with a detailed explanation of changes and screenshots (if UI is changing). Tests would be great too!
 - One of the core team members will review your changes.
 - After successful code review, you’ll receive a :+1: and the changes will be merged by one of the core members.

If you’re submitting a bug report, please try to follow these steps:
 - Search through the open and closed issues, maybe there is already an issue describing exactly the same problem.
 - Describe the issue as detailed as possible. Try to describe the expected and the actual outcome.
 - Add reproduction steps. If possible provide sample code that showcases the issue.
 - Provide a failing test.

### Documentation

[View](http://zdmobilesdkdocdev.herokuapp.com/belvedere/) | [Download](https://zendesk.artifactoryonline.com/zendesk/repo/com/zendesk/belvedere/1.2.0.1/belvedere-1.2.0.1-javadoc.jar)

### License
```
Copyright 2018 Zendesk

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
```

