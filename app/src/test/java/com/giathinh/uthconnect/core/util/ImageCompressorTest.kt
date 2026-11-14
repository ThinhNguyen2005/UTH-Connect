@file:Suppress("MagicNumber")

package com.giathinh.uthconnect.core.util

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.media.ExifInterface
import android.net.Uri
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.FileNotFoundException
import java.io.InputStream
import java.util.Random
import java.util.concurrent.atomic.AtomicInteger

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31], manifest = Config.NONE, application = Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ImageCompressorTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun webpMetadataSignatureAndDecodedDimensions() = runBlocking {
        val input = fixture(400, 300)
        val result = success(input)
        assertEquals("RIFF", String(result.bytes, 0, 4, Charsets.US_ASCII))
        assertEquals("WEBP", String(result.bytes, 8, 4, Charsets.US_ASCII))
        assertEquals("image/webp", result.mimeType)
        assertTrue(result.filename.matches(Regex("[0-9a-f-]{36}\\.webp")))
        assertEquals(result.bytes.size, result.byteSize)
        assertTrue(result.byteSize <= 153600)
        assertEquals(input.size.toLong(), result.inputByteSize)
        assertEquals(result.byteSize.toDouble() / input.size, result.compressionRatio, 0.0)
        checkDecoded(result) {
            assertEquals(result.width, it.width)
            assertEquals(result.height, it.height)
        }
    }

    @Test
    fun landscapeScalesTo1080PreservingAspectRatio() = runBlocking {
        val result = success(fixture(4000, 3000))
        assertEquals(1080, result.width)
        assertEquals(810, result.height)
    }

    @Test
    fun portraitScalesTo1080PreservingAspectRatio() = runBlocking {
        val result = success(fixture(3000, 4000))
        assertEquals(810, result.width)
        assertEquals(1080, result.height)
    }

    @Test
    fun smallAndOddSizedImagesAreNotUpscaled() = runBlocking {
        val small = success(fixture(73, 51))
        assertEquals(73, small.width)
        assertEquals(51, small.height)
        val odd = success(fixture(2001, 1333))
        assertTrue(kotlin.math.abs(odd.height - odd.width * 1333.0 / 2001) <= 1)
    }

    @Test
    fun exifRotationsAndMirrorsTransformPixels() = runBlocking {
        // Source quadrants: red, green / blue, yellow. Assert pixels, including 180-degree rotation.
        val expected = mapOf(
            ExifInterface.ORIENTATION_ROTATE_90 to listOf(Color.BLUE, Color.RED, Color.YELLOW, Color.GREEN),
            ExifInterface.ORIENTATION_ROTATE_180 to listOf(Color.YELLOW, Color.BLUE, Color.GREEN, Color.RED),
            ExifInterface.ORIENTATION_ROTATE_270 to listOf(Color.GREEN, Color.YELLOW, Color.RED, Color.BLUE),
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL to listOf(Color.GREEN, Color.RED, Color.YELLOW, Color.BLUE),
            ExifInterface.ORIENTATION_FLIP_VERTICAL to listOf(Color.BLUE, Color.YELLOW, Color.RED, Color.GREEN),
            ExifInterface.ORIENTATION_TRANSPOSE to listOf(Color.RED, Color.BLUE, Color.GREEN, Color.YELLOW),
            ExifInterface.ORIENTATION_TRANSVERSE to listOf(Color.YELLOW, Color.GREEN, Color.BLUE, Color.RED),
        )
        for ((orientation, colors) in expected) {
            val file = temporaryFolder.newFile("orientation-$orientation.jpg")
            file.writeBytes(fixture(160, 80))
            ExifInterface(file.path).apply {
                setAttribute(ExifInterface.TAG_ORIENTATION, orientation.toString())
                saveAttributes()
            }
            val result = ImageCompressor.compress(
                RuntimeEnvironment.getApplication().contentResolver,
                Uri.fromFile(file),
            )
            assertTrue(result is ImageCompressor.Result.Success)
            result as ImageCompressor.Result.Success
            val swapped = orientation in listOf(5, 6, 7, 8)
            assertEquals(if (swapped) 80 else 160, result.width)
            assertEquals(if (swapped) 160 else 80, result.height)
            checkDecoded(result) { bitmap ->
                val pixels = listOf(
                    bitmap.getPixel(bitmap.width / 4, bitmap.height / 4),
                    bitmap.getPixel(bitmap.width * 3 / 4, bitmap.height / 4),
                    bitmap.getPixel(bitmap.width / 4, bitmap.height * 3 / 4),
                    bitmap.getPixel(bitmap.width * 3 / 4, bitmap.height * 3 / 4),
                )
                pixels.zip(colors).forEach { (actual, wanted) ->
                    assertTrue(kotlin.math.abs(Color.red(actual) - Color.red(wanted)) < 35)
                    assertTrue(kotlin.math.abs(Color.green(actual) - Color.green(wanted)) < 35)
                    assertTrue(kotlin.math.abs(Color.blue(actual) - Color.blue(wanted)) < 35)
                }
            }
        }
    }

    @Test
    fun realisticLargeInputMeasuresActualCompressionRatio() = runBlocking {
        val input = fixture(2400, 1800, noise = true, quality = 95)
        assertTrue("Fixture size: ${input.size}", input.size in 4_000_000..6_500_000)
        val result = success(input)
        assertTrue(result.byteSize <= 153600)
        assertTrue(result.compressionRatio < 0.04)
        println("KAN-108 measured input=${input.size}, output=${result.byteSize}, ratio=${result.compressionRatio}")
    }

    @Test
    fun noiseForcesResolutionReductionAndStillRespectsLimit() = runBlocking {
        val result = success(fixture(1080, 1080, noise = true))
        assertTrue(result.byteSize <= 153600)
        assertTrue(result.width < 1080)
    }

    @Test
    fun impossibleBudgetReturnsFailure() = runBlocking {
        val input = fixture(400, 300, noise = true)
        val result = ImageCompressor.compress({ ByteArrayInputStream(input) }, ImageCompressor.Options(maxBytes = 1))
        assertEquals(ImageCompressor.Result.Failure(ImageCompressor.FailureReason.SIZE_LIMIT), result)
    }

    @Test
    fun corruptEmptyMissingAndDeniedSourcesFailSafely() = runBlocking {
        for (input in listOf(byteArrayOf(), byteArrayOf(1, 2, 3))) {
            assertEquals(
                ImageCompressor.Result.Failure(ImageCompressor.FailureReason.INVALID_IMAGE),
                ImageCompressor.compress({ ByteArrayInputStream(input) }),
            )
        }
        assertEquals(
            ImageCompressor.Result.Failure(ImageCompressor.FailureReason.UNREADABLE_INPUT),
            ImageCompressor.compress({ throw FileNotFoundException() }),
        )
        assertEquals(
            ImageCompressor.Result.Failure(ImageCompressor.FailureReason.UNREADABLE_INPUT),
            ImageCompressor.compress({ throw SecurityException() }),
        )
    }

    @Test
    fun repeatedImagesCloseEveryStream() = runBlocking {
        val input = fixture(200, 150)
        val opened = AtomicInteger()
        val closed = AtomicInteger()
        repeat(20) {
            val result = ImageCompressor.compress({
                opened.incrementAndGet()
                object : ByteArrayInputStream(input) {
                    override fun close() {
                        closed.incrementAndGet()
                        super.close()
                    }
                }
            })
            assertTrue(result is ImageCompressor.Result.Success)
            assertEquals(opened.get(), closed.get())
        }
    }

    @Test
    fun oversizedInputIsBoundedAndClosed() = runBlocking {
        var closed = false
        var bytesRead = 0L
        val result = ImageCompressor.compress({
            object : InputStream() {
                override fun read(): Int = 0
                override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
                    bytesRead += length
                    return length
                }
                override fun close() {
                    closed = true
                }
            }
        })
        assertEquals(ImageCompressor.Result.Failure(ImageCompressor.FailureReason.INPUT_TOO_LARGE), result)
        assertTrue(closed)
        assertTrue(bytesRead <= 32L * 1024 * 1024 + 8192)
    }

    @Test
    fun missingUriReturnsControlledFailure() = runBlocking {
        val file = temporaryFolder.root.resolve("missing.jpg")
        assertEquals(
            ImageCompressor.Result.Failure(ImageCompressor.FailureReason.UNREADABLE_INPUT),
            ImageCompressor.compress(RuntimeEnvironment.getApplication().contentResolver, Uri.fromFile(file)),
        )
    }

    @Test
    fun cancellationPropagatesAndClosesStream() = runBlocking {
        val started = CompletableDeferred<Unit>()
        val closed = AtomicInteger()
        val job = async {
            ImageCompressor.compress({
                object : ByteArrayInputStream(fixture(200, 150)) {
                    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
                        started.complete(Unit)
                        throw CancellationException("cancel during read")
                    }
                    override fun close() {
                        closed.incrementAndGet()
                        super.close()
                    }
                }
            })
        }
        started.await()
        try {
            job.await()
            throw AssertionError("Cancellation was swallowed")
        } catch (_: CancellationException) {
            assertTrue(job.isCancelled)
        }
        job.cancelAndJoin()
        assertEquals(1, closed.get())
    }

    @Test
    fun alphaIsPreserved() = runBlocking {
        val bitmap = Bitmap.createBitmap(80, 60, Bitmap.Config.ARGB_8888)
        val input = try {
            bitmap.eraseColor(Color.TRANSPARENT)
            bitmap.setPixel(40, 30, Color.RED)
            ByteArrayOutputStream().use {
                assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it))
                it.toByteArray()
            }
        } finally {
            bitmap.recycle()
        }
        checkDecoded(success(input)) { assertEquals(0, Color.alpha(it.getPixel(0, 0))) }
    }

    private suspend fun success(input: ByteArray): ImageCompressor.Result.Success {
        val result = ImageCompressor.compress({ ByteArrayInputStream(input) })
        assertTrue("Expected success: $result", result is ImageCompressor.Result.Success)
        return result as ImageCompressor.Result.Success
    }

    private fun checkDecoded(result: ImageCompressor.Result.Success, check: (Bitmap) -> Unit) {
        val decoded = requireNotNull(BitmapFactory.decodeByteArray(result.bytes, 0, result.bytes.size))
        try {
            assertFalse(decoded.isRecycled)
            check(decoded)
        } finally {
            decoded.recycle()
        }
    }

    private fun fixture(width: Int, height: Int, noise: Boolean = false, quality: Int = 100): ByteArray {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        try {
            val random = Random(108)
            val row = IntArray(width)
            repeat(height) { y ->
                repeat(width) { x ->
                    row[x] = if (noise) {
                        Color.rgb(random.nextInt(256), random.nextInt(256), random.nextInt(256))
                    } else if (y < height / 2) {
                        if (x < width / 2) Color.RED else Color.GREEN
                    } else {
                        if (x < width / 2) Color.BLUE else Color.YELLOW
                    }
                }
                bitmap.setPixels(row, 0, width, 0, y, width, 1)
            }
            return ByteArrayOutputStream().use {
                assertTrue(bitmap.compress(Bitmap.CompressFormat.JPEG, quality, it))
                it.toByteArray()
            }
        } finally {
            bitmap.recycle()
        }
    }
}
