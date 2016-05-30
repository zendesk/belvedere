## belvedere
Zero permissions file picker for Android.

## Overview
Belvedere gives you the power to easily integrate file selection from third party apps and the camera without the need to take care of permissions, ContentProvider, Intent permissions, and so on.

## Download
To use Belvedere in your own Android application, add the following maven repository:

```
repositories {
    maven { url 'https://zendesk.artifactoryonline.com/zendesk/repo' }
}
```

And add belvedere as a dependency:

```
compile ‘com.zendesk:belvedere:1.1.1.1’
```

Belvedere relies on a certain feature of the Android manifest merger called placeholder support. Please make sure to provide the package name of your app as `applicationId` in your module specific `build.gradle`.
For an example have a look at sample app.

If you’re not using Gradle, or you don’t have placeholder support please add the following to your `AndroidManifest.xml`:

```xml
<provider
    android:name="com.zendesk.belvedere.BelvedereFileProvider"
    android:authorities="<applicationId>.belvedere.attachments"
    android:exported="false"
    android:grantUriPermissions="true">

    <meta-data
        android:name="android.support.FILE_PROVIDER_PATHS"
        android:resource="@xml/belvedere_attachment_storage" />

</provider>
```

## How to use Belvedere

### Obtaining an instance
A Belvedere instance could be created as easily as the following:

```java
Belvedere belvedere = Belvedere.from(context)
                .withContentType("image/*")
                .build();
```

The newly created instance is used to acquire images from third party apps.

Belvedere requires that you create an instance once and reuse that instance. We recommend to keep an instance in your global application class, in a headless fragment, a singleton or use your DI to take care of a Belvedere instance.

For all the available configuration options, please have a look at our Javadoc:

## Display the built-in dialog
If you want to show the built-in dialog to let the user select a file, invoke the following:

```java
belvedere.showDialog(fragmentManager);
```

## Parsing the result
To get access to the selected files, put the following into your Fragment’s or Activity’s `onActivityResult()`.

```java
...
protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
    belvedere.getFilesFromActivityOnResult(requestCode, resultCode, data, new BelvedereCallback<List<BelvedereResult>>() {
        @Override
        public void success(final List<BelvedereResult> result) {
            // use data
        }
    });
}
...
```
All files the user selects are copied into your apps internal cache. As a result Belvedere will return you a list of `BelvedereResult` objects. Each of these objects represents one selected file. To get access call `BelvedereResult#getFile()` or `BelvedereResult#getUri()`, both of which point to the internal cache, so no permissions are needed to access them.

### Place a file into Belvedere’s internal storage
Moreover, it’s possible to put your own data into Belvedere’s cache. To get access to an internal file, call:

```
BelvedereResult file = belvedere.getFileRepresentation(“file_name.tmp”);
```
Again, you’ll get a file object and an Uri. For example, you can use the file to open a FileOutputStream.
Open or share an internal file
Files that are available through Belvedere could be opened or shared with other apps. To do that, use the Uri you get from a `BelvedereResult`.

Use the first code snippet to open a file and the second one to share a file:

```java
final Intent intent = new Intent(Intent.ACTION_VIEW);
intent.setDataAndType(uri, "image/*");
belvedere.grantPermissionsForUri(intent, uri);

startActivity(intent);
```

```java
final Intent shareIntent = new Intent();
shareIntent.setAction(Intent.ACTION_SEND);
shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
shareIntent.setType("image/*");
belvedere.grantPermissionsForUri(shareIntent, uri);

startActivity(shareIntent);
```


## Contributing

Bug reports, feature requests and contributions are very welcome. Please follow these steps to contribute:
 - Submit a Pull Request with a detailed explanation of changes and screenshots (if UI is changing). Tests would be great too!
 - One of the core team members will review your changes.
 - After successful code review, you’ll receive a :+1: and the changes will be merged by one of the core members.

If you’re submitting a bug report, please try to follow these steps:
 - Search through the open and closed issues, maybe there is already an issue describing exactly the same problem.
 - Describe the issue as detailed as possible. Try to describe the expected and the actual outcome.
 - Add reproduction steps. If possible provide sample code that showcases the issue.
 - Provide a failing test.

## Team
@schlan @baz8080 @brendan-fahy @a1cooke @ndobir @pmurph0

## Documentation
[JavaDoc](https://zendesk.artifactoryonline.com/zendesk/repo/com/zendesk/belvedere/1.1.1.1/belvedere-1.1.1.1-javadoc.jar)

## License
```
Copyright 2016 Zendesk

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
```

