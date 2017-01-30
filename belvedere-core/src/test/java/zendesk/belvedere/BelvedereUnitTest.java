package zendesk.belvedere;

import android.content.Context;
import android.content.pm.PackageManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
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
        when(mContext.getApplicationContext()).thenReturn(mContext);
    }

    @Test
    public void testBelvedere(){
        final Belvedere belvedere = Belvedere.from(mContext)
                .build();

        assertThat("Belvedere shouldn't be null", belvedere, is(notNullValue()));
    }
}