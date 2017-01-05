//package com.zendesk.belvedere;
//
//import android.app.Application;
//import android.content.Context;
//import android.content.Intent;
//import android.net.Uri;
//import android.os.Build;
//import android.provider.MediaStore;
//import android.test.ApplicationTestCase;
//
//import java.util.List;
//
//import static org.hamcrest.CoreMatchers.allOf;
//import static org.hamcrest.CoreMatchers.notNullValue;
//import static org.hamcrest.MatcherAssert.assertThat;
//import static org.hamcrest.Matchers.contains;
//import static org.hamcrest.Matchers.greaterThanOrEqualTo;
//import static org.hamcrest.Matchers.is;
//import static org.hamcrest.Matchers.lessThanOrEqualTo;
//
//public class BelvedereTest extends ApplicationTestCase<Application> {
//
//    public BelvedereTest() {
//        super(Application.class);
//    }
//
//    public void testDefaultConfig(){
//        final Context context = getContext();
//
//        final Belvedere build = Belvedere.from(context)
//                .build();
//
//        final List<BelvedereIntent> belvedereIntents = build.getBelvedereIntents();
//
//        assertThat("Belvedere should return a non null list", belvedereIntents, is(notNullValue()));
//        assertThat("Belvedere should return two intents", belvedereIntents.size(), is(2));
//
//        for(BelvedereIntent belvedereIntent : belvedereIntents) {
//            final Intent intent = belvedereIntent.getIntent();
//
//            switch (belvedereIntent.getSource()){
//                case Camera:
//                    assertThat("Action should be ACTION_IMAGE_CAPTURE", intent.getAction(), is(MediaStore.ACTION_IMAGE_CAPTURE));
//
//                    final Uri uri = intent.getParcelableExtra(MediaStore.EXTRA_OUTPUT);
//                    assertThat("Intent should have an EXTRA_OUTPUT", uri, is(notNullValue()));
//
//                    final String authority = uri.getAuthority();
//                    final String expectedAuthority = context.getPackageName() + context.getResources().getString(R.string.belvedere_sdk_fpa_suffix);
//                    assertThat("Authority must match", authority, is(expectedAuthority));
//
//                    break;
//
//                case Gallery:
//                    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT){
//                        assertThat("Action should be Action_OPEN_DOCUMENT", intent.getAction(), is(Intent.ACTION_OPEN_DOCUMENT));
//                    } else {
//                        assertThat("Action should be ACTION_GET_CONTENT", intent.getAction(), is(Intent.ACTION_GET_CONTENT));
//                    }
//
//                    assertThat("Default Content type should match", intent.getType(), is("*/*"));
//                    assertThat("Category should be CATEGORY_OPENABLE", intent.getCategories(), contains(Intent.CATEGORY_OPENABLE));
//
//                    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
//                        assertThat("Select multiple should be activated", intent.getBooleanExtra(Intent.EXTRA_ALLOW_MULTIPLE, false), is(true));
//                    }
//
//                    break;
//            }
//        }
//    }
//
//    public void testAllowMultiple(){
//        final Context context = getContext();
//
//        final Belvedere belvedere = Belvedere.from(context)
//                .withAllowMultiple(true)
//                .withSource(BelvedereSource.Gallery)
//                .build();
//
//        final List<BelvedereIntent> belvedereIntents = belvedere .getBelvedereIntents();
//
//        assertThat("Belvedere should return a non null list", belvedereIntents, is(notNullValue()));
//        assertThat("Belvedere should return one intent", belvedereIntents.size(), is(1));
//
//        final BelvedereIntent belvedereIntent = belvedereIntents.get(0);
//
//        assertThat("Intent should point to Gallery", belvedereIntent.getSource(), is(BelvedereSource.Gallery));
//
//        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2){
//            final Intent intent = belvedereIntent.getIntent();
//            final boolean allowMultiple = intent.getBooleanExtra(Intent.EXTRA_ALLOW_MULTIPLE, false);
//            assertThat("Intent should allow multiple", allowMultiple, is(true));
//        }
//    }
//
//    public void testCustomGalleryRequestIds(){
//        final Context context = getContext();
//        final int galleryRequestId = 666;
//
//        final Belvedere belvedere = Belvedere.from(context)
//                .withSource(BelvedereSource.Gallery)
//                .withGalleryRequestCode(galleryRequestId)
//                .build();
//
//        final List<BelvedereIntent> belvedereIntents = belvedere.getBelvedereIntents();
//
//        assertThat("Belvedere should return a non null list", belvedereIntents, is(notNullValue()));
//        assertThat("Belvedere should return one intent", belvedereIntents.size(), is(1));
//
//        final BelvedereIntent belvedereIntent = belvedereIntents.get(0);
//
//        assertThat("Request Id isn't equal", belvedereIntent.getRequestCode(), is(galleryRequestId));
//    }
//
//    public void testCustomCameraRequestIds() {
//        final Context context = getContext();
//        final int cameraRequestIdStart = 100;
//        final int cameraRequestIdEnd = 110;
//
//        final Belvedere belvedere = Belvedere.from(context)
//                .withSource(BelvedereSource.Camera)
//                .withCameraRequestCode(cameraRequestIdStart, cameraRequestIdEnd)
//                .build();
//
//        final List<BelvedereIntent> belvedereIntents = belvedere.getBelvedereIntents();
//
//        assertThat("Belvedere should return a non null list", belvedereIntents, is(notNullValue()));
//        assertThat("Belvedere should return one intent", belvedereIntents.size(), is(1));
//
//        final BelvedereIntent belvedereIntent = belvedereIntents.get(0);
//
//        assertThat(
//                "Request Id isn't between provided start and end id",
//                belvedereIntent.getRequestCode(),
//                allOf(greaterThanOrEqualTo(cameraRequestIdStart), lessThanOrEqualTo(cameraRequestIdEnd))
//        );
//    }
//
//    public void testCustomContentType(){
//        final Context context = getContext();
//
//        final String contentType = "image/*";
//
//        final Belvedere belvedere = Belvedere.from(context)
//                .withSource(BelvedereSource.Gallery)
//                .withContentType(contentType)
//                .build();
//
//        final List<BelvedereIntent> belvedereIntents = belvedere.getBelvedereIntents();
//        assertThat("Belvedere should return a non null list", belvedereIntents, is(notNullValue()));
//        assertThat("Belvedere should return one intent", belvedereIntents.size(), is(1));
//
//        final BelvedereIntent belvedereIntent = belvedereIntents.get(0);
//        final Intent intent = belvedereIntent.getIntent();
//        assertThat("Content type should match", intent.getType(), is(contentType));
//    }
//
//    public void testFaultyConfig(){
//        final Context context = getContext();
//
//        try{
//            Belvedere.from(context)
//                    .withSource()
//                    .build();
//            fail("Empty source list not allowed - shouldn't pass");
//        } catch (IllegalArgumentException e) {
//            // Intentionally empty
//        }
//
//        try{
//            Belvedere.from(context)
//                    .withCameraRequestCode(10, 1)
//                    .build();
//            fail("CameraRequestCode: Invalid range provided - shouldn't pass");
//        } catch (IllegalArgumentException e) {
//            // Intentionally empty
//        }
//
//        try {
//            Belvedere.from(null);
//            fail("Init with null context - shouldn't pass");
//        } catch (IllegalArgumentException e){
//            // Intentionally empty
//        }
//
//        try{
//            Belvedere.from(context)
//                    .withCustomLogger(null)
//                    .build();
//            fail("Init with null Logger - shouldn't pass");
//        } catch (IllegalArgumentException e){
//            // Intentionally empty
//        }
//    }
//
//    public void testGetFile(){
//        final Context context = getContext();
//        final String fileName = "test.tmp";
//
//        final Belvedere belvedere = Belvedere.from(context)
//                .build();
//
//        final BelvedereResult file = belvedere.getFile(fileName);
//        assertThat("File shouldn't be null", file, is(notNullValue()));
//        assertThat("Uri shouldn't be null", file.getUri(), is(notNullValue()));
//        assertThat("File shouldn't be null", file.getFile(), is(notNullValue()));
//
//        final String authority = file.getUri().getAuthority();
//        final String expectedAuthority = context.getPackageName() + context.getResources().getString(R.string.belvedere_sdk_fpa_suffix);
//        assertThat("Authority should match", authority, is(expectedAuthority));
//    }
//
//}