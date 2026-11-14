package com.giathinh.uthconnect.core.util

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.UUID
import kotlin.coroutines.coroutineContext
import kotlin.math.roundToInt

/** Serializes decoding to bound peak memory. Never retains a Context or caller-owned Bitmap. */
object ImageCompressor {
    const val MAX_BYTES = 153600
    const val MAX_EDGE = 1080
    private const val MAX_INPUT_BYTES = 32L * 1024 * 1024
    private const val INITIAL_QUALITY = 80
    private const val MIN_QUALITY = 75
    private const val RESOLUTION_FACTOR = 0.75
    private const val MAX_RESOLUTION_ATTEMPTS = 6
    private const val ROTATE_90 = 90f
    private const val ROTATE_180 = 180f
    private const val ROTATE_270 = 270f
    private val processing = Mutex()

    data class Options(
        val maxEdge: Int = MAX_EDGE,
        val maxBytes: Int = MAX_BYTES,
        val minEdge: Int = 256,
    ) {
        init {
            require(maxEdge in 1..MAX_EDGE)
            require(maxBytes in 1..MAX_BYTES)
            require(minEdge in 1..maxEdge)
        }
    }

    sealed interface Result {
        /** Alpha is preserved by WebP lossy; ratio is output bytes / input bytes. */
        class Success internal constructor(
            val bytes: ByteArray,
            val width: Int,
            val height: Int,
            val inputByteSize: Long,
        ) : Result {
            val filename: String = "${UUID.randomUUID()}.webp"
            val mimeType: String = "image/webp"
            val byteSize: Int get() = bytes.size
            val compressionRatio: Double get() = byteSize.toDouble() / inputByteSize
        }

        data class Failure(val reason: FailureReason) : Result
    }

    enum class FailureReason {
        UNREADABLE_INPUT,
        INVALID_IMAGE,
        INPUT_TOO_LARGE,
        ENCODE_FAILED,
        SIZE_LIMIT,
        OUT_OF_MEMORY,
    }

    suspend fun compress(
        resolver: ContentResolver,
        uri: Uri,
        options: Options = Options(),
    ): Result = compress(
        { resolver.openInputStream(uri) ?: throw IOException("Cannot open image") },
        options,
    )

    // A fresh stream on each call allows bounds/EXIF/decode without buffering the full source.
    internal suspend fun compress(
        openStream: () -> InputStream,
        options: Options = Options(),
    ): Result = withContext(Dispatchers.IO) {
        processing.withLock {
            try {
                val inputSize = measureInput(openStream)
                val orientation = readOrientation(openStream)
                encode(openStream, orientation, inputSize, options)
            } catch (error: CompressionException) {
                Result.Failure(error.reason)
            } catch (_: IOException) {
                Result.Failure(FailureReason.UNREADABLE_INPUT)
            } catch (_: SecurityException) {
                Result.Failure(FailureReason.UNREADABLE_INPUT)
            } catch (_: OutOfMemoryError) {
                // Owned bitmaps are released by finally; do not retry under memory pressure.
                Result.Failure(FailureReason.OUT_OF_MEMORY)
            }
        }
    }

    private suspend fun measureInput(openStream: () -> InputStream): Long = openStream().use { input ->
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var total = 0L
        var count = input.read(buffer)
        while (count != -1) {
            coroutineContext.ensureActive()
            total += count
            if (total > MAX_INPUT_BYTES) throw CompressionException(FailureReason.INPUT_TOO_LARGE)
            count = input.read(buffer)
        }
        if (total == 0L) throw CompressionException(FailureReason.INVALID_IMAGE)
        total
    }

    private fun readOrientation(openStream: () -> InputStream): Int = try {
        openStream().use {
            ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        }
    } catch (_: IOException) {
        // Formats without EXIF can still be valid BitmapFactory inputs.
        ExifInterface.ORIENTATION_NORMAL
    }

    private fun decode(openStream: () -> InputStream, maxEdge: Int): Bitmap {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        openStream().use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) throw CompressionException(FailureReason.INVALID_IMAGE)
        var sample = 1
        val longest = maxOf(bounds.outWidth, bounds.outHeight)
        while (longest / sample / 2 >= maxEdge) sample *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        return openStream().use { BitmapFactory.decodeStream(it, null, options) }
            ?: throw CompressionException(FailureReason.INVALID_IMAGE)
    }

    private suspend fun encode(
        openStream: () -> InputStream,
        orientation: Int,
        inputSize: Long,
        options: Options,
    ): Result {
        var bitmap = decode(openStream, options.maxEdge)
        try {
            bitmap = bitmap.replaceWith(scale(bitmap, options.maxEdge))
            bitmap = bitmap.replaceWith(orient(bitmap, orientation))
            repeat(MAX_RESOLUTION_ATTEMPTS) {
                for (quality in INITIAL_QUALITY downTo MIN_QUALITY step INITIAL_QUALITY - MIN_QUALITY) {
                    coroutineContext.ensureActive()
                    val bytes = ByteArrayOutputStream().use { output ->
                        if (!bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, quality, output)) {
                            throw CompressionException(FailureReason.ENCODE_FAILED)
                        }
                        output.toByteArray()
                    }
                    coroutineContext.ensureActive()
                    if (bytes.size <= options.maxBytes) {
                        validateOutput(bytes, bitmap.width, bitmap.height)
                        return Result.Success(bytes, bitmap.width, bitmap.height, inputSize)
                    }
                }
                val longest = maxOf(bitmap.width, bitmap.height)
                if (longest <= options.minEdge) throw CompressionException(FailureReason.SIZE_LIMIT)
                val nextEdge = (longest * RESOLUTION_FACTOR).toInt().coerceAtLeast(options.minEdge)
                bitmap = bitmap.replaceWith(scale(bitmap, nextEdge))
            }
            return Result.Failure(FailureReason.SIZE_LIMIT)
        } finally {
            bitmap.recycle()
        }
    }

    private fun scale(bitmap: Bitmap, edge: Int): Bitmap {
        val longest = maxOf(bitmap.width, bitmap.height)
        if (longest <= edge) return bitmap
        val factor = edge.toDouble() / longest
        return Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * factor).roundToInt().coerceAtLeast(1),
            (bitmap.height * factor).roundToInt().coerceAtLeast(1),
            true,
        )
    }

    private fun orient(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.setScale(-1f, 1f)

            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.setScale(1f, -1f)

            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.setRotate(ROTATE_90)

            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.setRotate(ROTATE_180)

            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.setRotate(ROTATE_270)

            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.setRotate(ROTATE_90)
                matrix.postScale(-1f, 1f)
            }

            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.setRotate(ROTATE_270)
                matrix.postScale(-1f, 1f)
            }

            else -> return bitmap
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun Bitmap.replaceWith(replacement: Bitmap): Bitmap {
        if (replacement !== this) recycle()
        return replacement
    }

    private fun validateOutput(bytes: ByteArray, width: Int, height: Int) {
        val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            ?: throw CompressionException(FailureReason.ENCODE_FAILED)
        try {
            if (decoded.width != width || decoded.height != height) {
                throw CompressionException(FailureReason.ENCODE_FAILED)
            }
        } finally {
            decoded.recycle()
        }
    }

    private class CompressionException(val reason: FailureReason) : IOException()
}
