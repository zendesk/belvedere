@file:Suppress("IllegalIdentifier")

package zendesk.belvedere

import android.content.Intent
import android.net.Uri
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner


@RunWith(MockitoJUnitRunner::class)
class ImageStreamModelTest : TestHelper {

    private val maxFileSize = 100L

    @Mock
    private lateinit var service: ImageStreamService

    private lateinit var model: ImageStreamModel

    private val selectedItems = mutableListOf<MediaResult>()

    private val additionalItems = mutableListOf<MediaResult>()

    private val mediaIntent = mutableListOf<MediaIntent>()

    @Before
    fun setup() {
        model = ImageStreamModel(service, maxFileSize, mediaIntent, selectedItems, additionalItems)
    }

    @After
    fun teardown() {
        selectedItems.clear()
        mediaIntent.clear()
        additionalItems.clear()
    }

    @Test
    fun `get images - 6 unique files`() {
        selectedItems.addAll(listOf(mediaResult(), mediaResult()))
        additionalItems.addAll(listOf(mediaResult(), mediaResult()))

        val imagesFromSystem = listOf(mediaResult(), mediaResult())
        `when`(service.queryRecentImages(anyInt())).thenReturn(imagesFromSystem)

        val images = model.latestImages
        assertThat(images).hasSize(6)

        // ensure order
        assertThat(images[0]).isEqualTo(selectedItems[0])
        assertThat(images[1]).isEqualTo(selectedItems[1])
        assertThat(images[2]).isEqualTo(additionalItems[0])
        assertThat(images[3]).isEqualTo(additionalItems[1])
        assertThat(images[4]).isEqualTo(imagesFromSystem[0])
        assertThat(images[5]).isEqualTo(imagesFromSystem[1])
    }

    @Test
    fun `get images - 6 files provided, 4 unique files`() {
        val uri1 = mock(Uri::class.java)
        val uri2 = mock(Uri::class.java)

        selectedItems.addAll(listOf(mediaResult(uri = uri1), mediaResult(uri = uri2)))
        additionalItems.addAll(listOf(mediaResult(uri = uri1), mediaResult(uri = uri2)))

        val imagesFromSystem = listOf(mediaResult(), mediaResult())
        `when`(service.queryRecentImages(anyInt())).thenReturn(imagesFromSystem)

        val images = model.latestImages
        assertThat(images).hasSize(4)

        // ensure order
        assertThat(images[0]).isEqualTo(additionalItems[0])
        assertThat(images[1]).isEqualTo(additionalItems[1])
        assertThat(images[2]).isEqualTo(imagesFromSystem[0])
        assertThat(images[3]).isEqualTo(imagesFromSystem[1])
    }

    @Test
    fun `get images - 0 files available`() {
        val imagesFromSystem = listOf<MediaResult>()
        `when`(service.queryRecentImages(anyInt())).thenReturn(imagesFromSystem)

        val images = model.latestImages
        assertThat(images).hasSize(0)
    }

    @Test
    fun `get max file size`() {
        assertThat(model.maxFileSize).isEqualTo(maxFileSize)
    }

    @Test
    fun `remove selected item`(){
        val resultToDelete = mediaResult()
        selectedItems.addAll(listOf(mediaResult(), resultToDelete, mediaResult()))

        val newList = model.removeFromSelectedItems(resultToDelete)

        assertThat(newList).hasSize(2)
        assertThat(newList).doesNotContain(resultToDelete)
    }

    @Test
    fun `add selected item`(){
        val itemToAdd = mediaResult()
        selectedItems.addAll(listOf(mediaResult(), mediaResult()))

        val newList = model.addToSelectedItems(itemToAdd)

        assertThat(newList).hasSize(3)
        assertThat(newList).contains(itemToAdd)
    }

    @Test
    fun `get selected item`(){
        assertThat(model.selectedMediaResults).isEqualTo(selectedItems)
    }

    @Test
    fun `has document or camera intent`() {
        mediaIntent.addAll(listOf(mediaIntent(MediaIntent.TARGET_CAMERA), mediaIntent(MediaIntent.TARGET_DOCUMENT)))

        assertThat(model.hasDocumentIntent()).isTrue()
        assertThat(model.hasCameraIntent()).isTrue()
    }

    @Test
    fun `has no document or camera intent`() {
        mediaIntent.addAll(listOf(mediaIntent(6), mediaIntent(6)))

        assertThat(model.hasDocumentIntent()).isFalse()
        assertThat(model.hasCameraIntent()).isFalse()
    }

    @Test
    fun `has no document or camera intent - empty list`() {
        assertThat(model.hasDocumentIntent()).isFalse()
        assertThat(model.hasCameraIntent()).isFalse()
    }

    @Test
    fun `get document intent`() {
        val documentIntent = mediaIntent(MediaIntent.TARGET_DOCUMENT)
        mediaIntent.addAll(listOf(mediaIntent(MediaIntent.TARGET_CAMERA), documentIntent))

        assertThat(model.documentIntent).isEqualTo(documentIntent)
    }

    @Test
    fun `get camera intent`() {
        val cameraIntent = mediaIntent(MediaIntent.TARGET_CAMERA)
        mediaIntent.addAll(listOf(mediaIntent(MediaIntent.TARGET_DOCUMENT), cameraIntent))

        assertThat(model.cameraIntent).isEqualTo(cameraIntent)
    }

    @Test
    fun `get document intent - not available`() {
        mediaIntent.addAll(listOf(mediaIntent(MediaIntent.TARGET_CAMERA)))

        assertThat(model.documentIntent).isNull()
    }

    @Test
    fun `get camera intent - not available`() {
        mediaIntent.addAll(listOf(mediaIntent(MediaIntent.TARGET_DOCUMENT)))

        assertThat(model.cameraIntent).isNull()
    }

    @Test
    fun `get google photos intent`() {
        val intent = mock(Intent::class.java)
        val documentIntent = mediaIntent(MediaIntent.TARGET_DOCUMENT, intent)
        mediaIntent.add(documentIntent)

        assertThat(model.googlePhotosIntent).isEqualTo(documentIntent)

        verify(intent, times(1)).`package` = "com.google.android.apps.photos"
        verify(intent, times(1)).action = Intent.ACTION_GET_CONTENT
    }

}