package com.zendesk.belveder;

import android.content.Context;
import android.content.pm.PackageManager;

import com.zendesk.belvedere.Belvedere;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * To work on unit tests, switch the Test Artifact in the Build Variants view.
 */
@RunWith(MockitoJUnitRunner.class)
public class BelvedereUnitTest {

    @Mock
    Context mContext;

    @Before
    public void setup(){
        final PackageManager mockPackageManager = mock(PackageManager.class);
        when(mContext.getPackageManager()).thenReturn(mockPackageManager);
        when(mContext.getApplicationContext()).thenReturn(mContext);
    }

    @Test
    public void testBelvedere(){
        final Belvedere belvedere = Belvedere.from(mContext)
                .build();

        assertThat("Belvedere shouldn't be null", belvedere, is(notNullValue()));
    }
}