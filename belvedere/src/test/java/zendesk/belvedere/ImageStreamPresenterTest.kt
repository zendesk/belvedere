@file:Suppress("IllegalIdentifier")

package zendesk.belvedere

import android.view.View
import com.google.common.truth.Truth
import com.google.common.truth.Truth.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner


@RunWith(MockitoJUnitRunner::class)
class ImageStreamPresenterTest : TestHelper {

    @Mock
    private lateinit var model: ImageStreamMvp.Model

    @Mock
    private lateinit var view: ImageStreamMvp.View

    @Mock
    private lateinit var imageStreamBackend: ImageStream

    private lateinit var presenter: ImageStreamMvp.Presenter

    @Before
    fun setup() {
        presenter = ImageStreamPresenter(model, view, imageStreamBackend)
    }

    @Test
    fun `init presenter - menu - photos and document available`() {
        `when`(model.hasDocumentIntent()).thenReturn(true)
        `when`(model.hasGooglePhotosIntent()).thenReturn(true)

        presenter.init()

        verify(view, times(1)).showDocumentMenuItem(any())
        verify(view, times(1)).showGooglePhotosMenuItem(any())
    }

    @Test
    fun `init presenter - menu - photos available`() {
        `when`(model.hasDocumentIntent()).thenReturn(false)
        `when`(model.hasGooglePhotosIntent()).thenReturn(true)

        presenter.init()

        verify(view, never()).showDocumentMenuItem(any())
        verify(view, times(1)).showGooglePhotosMenuItem(any())
    }

    @Test
    fun `init presenter - menu - document available`() {
        `when`(model.hasDocumentIntent()).thenReturn(true)
        `when`(model.hasGooglePhotosIntent()).thenReturn(false)

        presenter.init()

        verify(view, times(1)).showDocumentMenuItem(any())
        verify(view, never()).showGooglePhotosMenuItem(any())
    }

    @Test
    fun `init presenter - menu - no intents available`() {
        `when`(model.hasDocumentIntent()).thenReturn(false)
        `when`(model.hasGooglePhotosIntent()).thenReturn(false)

        presenter.init()

        verify(view, never()).showDocumentMenuItem(any())
        verify(view, never()).showGooglePhotosMenuItem(any())
    }

    @Test
    fun `init presenter - stream`() {
        val latestImages = listOf(mediaResult(), mediaResult())
        val selectedImages = listOf(latestImages[0])

        `when`(model.latestImages).thenReturn(latestImages)
        `when`(model.selectedMediaResults).thenReturn(selectedImages)
        `when`(model.hasCameraIntent()).thenReturn(true)

        presenter.init()

        verify(view, times(1)).initViews()
        verify(view, times(1)).showImageStream(eq(latestImages), eq(selectedImages), eq(true), any())
        verify(imageStreamBackend, times(1)).notifyVisible()
    }

    @Test
    fun `item click listener - open camera`() {
        val cameraIntent = mediaIntent(MediaIntent.TARGET_CAMERA)
        `when`(model.hasCameraIntent()).thenReturn(true)
        `when`(model.cameraIntent).thenReturn(cameraIntent)

        val listener = listener()
        listener.onOpenCamera()

        verify(view, times(1)).openMediaIntent(eq(cameraIntent), eq(imageStreamBackend))
    }

    @Test
    fun `item click listener - select item, no max file size`() {
        val mediaResult = mediaResult(size = 10)
        val item = spy(TestItem(mediaResult))

        `when`(model.maxFileSize).thenReturn(-1L)
        `when`(model.addToSelectedItems(any())).thenReturn(listOf(mediaResult))

        val listener = listener()
        listener.onSelectionChanged(item)

        assertThat(item.isSelected).isTrue()
        verify(model, times(1)).addToSelectedItems(eq(mediaResult))
        verify(imageStreamBackend, times(1)).notifyImageSelected(eq(listOf(mediaResult)), eq(true))
    }

    @Test
    fun `item click listener - deselect item`() {
        val mediaResult = mediaResult(size = 10)
        val item = spy(TestItem(mediaResult))
        item.isSelected = true

        `when`(model.maxFileSize).thenReturn(-1L)
        `when`(model.removeFromSelectedItems(any())).thenReturn(listOf())

        val listener = listener()
        listener.onSelectionChanged(item)

        assertThat(item.isSelected).isFalse()
        verify(model, times(1)).removeFromSelectedItems(eq(mediaResult))
        verify(imageStreamBackend, times(1)).notifyImageSelected(eq(listOf()), eq(true))
    }

    @Test
    fun `item click listener - select item, max file size, item is smaller`() {
        val mediaResult = mediaResult(size = 9)
        val item = spy(TestItem(mediaResult))

        `when`(model.maxFileSize).thenReturn(10L)
        `when`(model.addToSelectedItems(any())).thenReturn(listOf(mediaResult))

        val listener = listener()
        listener.onSelectionChanged(item)

        assertThat(item.isSelected).isTrue()
        verify(model, times(1)).addToSelectedItems(eq(mediaResult))
        verify(imageStreamBackend, times(1)).notifyImageSelected(eq(listOf(mediaResult)), eq(true))
    }

    @Test
    fun `item click listener - select item, max file size, item is bigger`() {
        val mediaResult = mediaResult(size = 10)
        val item = spy(TestItem(mediaResult))

        `when`(model.maxFileSize).thenReturn(9L)

        val listener = listener()
        listener.onSelectionChanged(item)

        assertThat(item.isSelected).isFalse()
        verify(view, times(1)).showToast(anyInt())
    }

    @Test
    fun `dismiss stream`() {
        presenter.dismiss()

        verify(imageStreamBackend, times(1)).setImageStreamUi(eq(null), eq(null))
        verify(imageStreamBackend, times(1)).notifyScrollListener(eq(0), eq(0), eq(0F))
        verify(imageStreamBackend, times(1)).notifyDismissed()
    }

    @Test
    fun `stream scrolled`() {
        presenter.onImageStreamScrolled(10, 10, 0F)

        verify(imageStreamBackend, times(1)).notifyScrollListener(eq(10), eq(10), eq(0F))
    }

    @Test
    fun `stream scrolled - over scroll`() {
        presenter.onImageStreamScrolled(10, 10, -10F)

        verify(imageStreamBackend, never()).notifyScrollListener(anyInt(), anyInt(), anyFloat())
    }

    private fun listener(): ImageStreamAdapter.Listener {
        val captor = ArgumentCaptor.forClass(ImageStreamAdapter.Listener::class.java)
        presenter.init()
        verify(view, times(1)).showImageStream(anyList(), anyList(), anyBoolean(), captor.capture())
        return captor.value
    }

    private open class TestItem(mediaResult: MediaResult): ImageStreamItems.Item(1, mediaResult) {
        override fun bind(view: View?) {

        }
    }

}