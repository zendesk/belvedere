//package com.zendesk.belvedere;
//
//import android.Manifest;
//import android.app.Application;
//import android.content.Intent;
//import android.net.Uri;
//import android.os.Bundle;
//import android.test.ApplicationTestCase;
//
//import java.io.File;
//
//import static org.hamcrest.CoreMatchers.is;
//import static org.hamcrest.CoreMatchers.notNullValue;
//import static org.hamcrest.MatcherAssert.assertThat;
//
//public class ParcelableTests extends ApplicationTestCase<Application> {
//
//    public ParcelableTests() {
//        super(Application.class);
//    }
//
//    public void testBelvedereIntent(){
//        final Bundle bundle = new Bundle();
//
//        final Intent mockIntent = new Intent();
//        final int requestId = 123;
//        final BelvedereSource source = BelvedereSource.Gallery;
//        final String permission = Manifest.permission.CAMERA;
//
//        final BelvedereIntent belvedereIntent = new BelvedereIntent(mockIntent, requestId, source, permission);
//        bundle.putParcelable("test", belvedereIntent);
//
//        final BelvedereIntent intent = bundle.getParcelable("test");
//        assertThat("BelvedereIntent shouldn't be null", intent, is(notNullValue()));
//        assertThat("BelvedereIntent should have content", intent.getRequestCode(), is(requestId));
//        assertThat("BelvedereIntent should have content", intent.getIntent(), is(mockIntent));
//        assertThat("BelvedereIntent should have content", intent.getSource(), is(source));
//        assertThat("BelvedereIntent should have content", intent.getPermission(), is(permission));
//    }
//
//
//    public void testBelvedereResult(){
//        final Bundle bundle = new Bundle();
//
//        final File file = new File("tmp");
//        final Uri uri = Uri.fromFile(file);
//
//        final BelvedereResult belvedereResult = new BelvedereResult(file, uri);
//
//        bundle.putParcelable("test", belvedereResult);
//
//        final BelvedereResult result = bundle.getParcelable("test");
//        assertThat("BelvedereResult shouldn't be null", result, is(notNullValue()));
//        assertThat("BelvedereResult should have content", result.getFile(), is(file));
//        assertThat("BelvedereResult should have content", result.getUri(), is(Uri.fromFile(file)));
//    }
//
//}