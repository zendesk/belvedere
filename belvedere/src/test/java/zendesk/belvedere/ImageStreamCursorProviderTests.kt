package zendesk.belvedere


import android.content.ContentResolver
import android.content.Context
import android.os.Bundle
import android.provider.MediaStore
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class ImageStreamCursorProviderTests {

    @Mock
    private lateinit var context: Context

    @Mock
    private lateinit var contentResolver: ContentResolver

    private lateinit var cursorProvider: ImageStreamCursorProvider

    @Before
    fun setUp() {
        `when`(context.contentResolver).thenReturn(contentResolver)
    }

    @Test
    fun `projection array has the expected parameters`() {
        assertThat(ImageStreamCursorProvider.PROJECTION).hasLength(5)

        assertThat(ImageStreamCursorProvider.PROJECTION[0])
                .isEqualTo(MediaStore.Images.ImageColumns._ID)

        assertThat(ImageStreamCursorProvider.PROJECTION[1])
                .isEqualTo(MediaStore.MediaColumns.DISPLAY_NAME)

        assertThat(ImageStreamCursorProvider.PROJECTION[2])
                .isEqualTo(MediaStore.MediaColumns.SIZE)

        assertThat(ImageStreamCursorProvider.PROJECTION[3])
                .isEqualTo(MediaStore.MediaColumns.WIDTH)

        assertThat(ImageStreamCursorProvider.PROJECTION[4])
                .isEqualTo(MediaStore.MediaColumns.HEIGHT)
    }

    @Test
    fun `order column is DATE_MODIFIED for android P (API 28)`() {
        cursorProvider = ImageStreamCursorProvider(context, 28)
        assertThat(cursorProvider.orderColumn).isEqualTo(MediaStore.Images.ImageColumns.DATE_MODIFIED)
    }

    @Test
    fun `order column is DATE_TAKEN for android Q (API 29)`() {
        cursorProvider = ImageStreamCursorProvider(context, 29)
        assertThat(cursorProvider.orderColumn).isEqualTo(MediaStore.Images.ImageColumns.DATE_TAKEN)
    }

    @Test
    fun `order column is DATE_TAKEN for android R (API 30)`() {
        cursorProvider = ImageStreamCursorProvider(context, 30)
        assertThat(cursorProvider.orderColumn).isEqualTo(MediaStore.Images.ImageColumns.DATE_TAKEN)
    }

    @Test
    fun `content provider is queried with a bundle for android 0 (API 26)`() {
        cursorProvider = ImageStreamCursorProvider(context, 26)
        cursorProvider.getCursor(5)

        verify(context.contentResolver, times(1)).query(
                any(),
                any(Array<String>::class.java),
                any(Bundle::class.java),
                eq(null))
    }

    @Test
    fun `content provider is queried without a bundle for android N (API 24 and 25)`() {
        cursorProvider = ImageStreamCursorProvider(context, 24)
        cursorProvider.getCursor(11)

        verify(context.contentResolver, times(1)).query(
                any(),
                any(Array<String>::class.java),
                eq(null),
                eq(null),
                eq("date_modified DESC LIMIT 11"))

        cursorProvider = ImageStreamCursorProvider(context, 25)
        cursorProvider.getCursor(9)

        verify(context.contentResolver, times(1)).query(
                any(),
                any(Array<String>::class.java),
                eq(null),
                eq(null),
                eq("date_modified DESC LIMIT 9"))
    }
}
