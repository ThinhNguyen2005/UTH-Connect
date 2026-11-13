package com.giathinh.uthconnect.presentation.lostfound

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

internal const val MAX_IMAGE_INPUT_BYTES = 15 * 1024 * 1024
internal const val MAX_IMAGE_DIMENSION = 1280
internal const val IMAGE_COMPRESSION_QUALITY = 80
private const val STREAM_BUFFER_CHUNK_SIZE = 8192
private const val ROTATION_90 = 90f
private const val ROTATION_180 = 180f
private const val ROTATION_270 = 270f

private fun readImageBytesBounded(
    context: Context,
    uri: Uri,
    maxBytes: Int,
): Pair<ByteArray?, Boolean> {
    val buffer = ByteArrayOutputStream()
    val stream = context.contentResolver.openInputStream(uri) ?: return Pair(null, false)
    val isTooLarge = stream.use { input ->
        val chunk = ByteArray(STREAM_BUFFER_CHUNK_SIZE)
        var total = 0
        var read = input.read(chunk)
        while (read != -1 && total + read <= maxBytes) {
            total += read
            buffer.write(chunk, 0, read)
            read = input.read(chunk)
        }
        read != -1
    }
    return Pair(if (isTooLarge) null else buffer.toByteArray(), isTooLarge)
}

private fun decodeExifOrientation(bytes: ByteArray): Int = runCatching {
    ByteArrayInputStream(bytes).use { exifStream ->
        val exif = ExifInterface(exifStream)
        exif.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL,
        )
    }
}.getOrDefault(ExifInterface.ORIENTATION_NORMAL)

private fun computeSampleSize(options: BitmapFactory.Options, maxDimension: Int): Int {
    var sampleSize = 1
    while (options.outWidth / sampleSize > maxDimension || options.outHeight / sampleSize > maxDimension) {
        sampleSize *= 2
    }
    return sampleSize
}

private fun applyExifTransformations(matrix: Matrix, orientation: Int): Boolean = when (orientation) {
    ExifInterface.ORIENTATION_ROTATE_90 -> {
        matrix.postRotate(ROTATION_90)
        true
    }

    ExifInterface.ORIENTATION_ROTATE_180 -> {
        matrix.postRotate(ROTATION_180)
        true
    }

    ExifInterface.ORIENTATION_ROTATE_270 -> {
        matrix.postRotate(ROTATION_270)
        true
    }

    ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> {
        matrix.postScale(-1f, 1f)
        true
    }

    ExifInterface.ORIENTATION_FLIP_VERTICAL -> {
        matrix.postScale(1f, -1f)
        true
    }

    ExifInterface.ORIENTATION_TRANSPOSE -> {
        matrix.postRotate(ROTATION_90)
        matrix.postScale(-1f, 1f)
        true
    }

    ExifInterface.ORIENTATION_TRANSVERSE -> {
        matrix.postRotate(ROTATION_270)
        matrix.postScale(-1f, 1f)
        true
    }

    else -> false
}

private fun scaleAndRotate(
    bitmap: Bitmap,
    orientation: Int,
    maxDimension: Int,
): Bitmap {
    val maxDim = maxOf(bitmap.width, bitmap.height)
    val scaled = if (maxDim > maxDimension) {
        val scale = maxDimension.toFloat() / maxDim
        val targetWidth = (bitmap.width * scale).toInt().coerceAtLeast(1)
        val targetHeight = (bitmap.height * scale).toInt().coerceAtLeast(1)
        Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true).also {
            if (it != bitmap) bitmap.recycle()
        }
    } else {
        bitmap
    }

    val matrix = Matrix()
    val hasTransformation = applyExifTransformations(matrix, orientation)
    return if (hasTransformation) {
        Bitmap.createBitmap(scaled, 0, 0, scaled.width, scaled.height, matrix, true).also {
            if (it != scaled) scaled.recycle()
        }
    } else {
        scaled
    }
}

private fun decodeSampledBitmap(rawBytes: ByteArray, maxDimension: Int): Bitmap? {
    val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, boundsOptions)
    if (boundsOptions.outWidth <= 0 || boundsOptions.outHeight <= 0) return null

    val decodeOptions = BitmapFactory.Options().apply {
        inSampleSize = computeSampleSize(boundsOptions, maxDimension)
    }
    return BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, decodeOptions)
}

private fun compressRawBytes(
    rawBytes: ByteArray,
    maxDimension: Int,
    quality: Int,
): ImageProcessResult {
    val orientation = decodeExifOrientation(rawBytes)
    val sampledBitmap = decodeSampledBitmap(rawBytes, maxDimension)
        ?: return ImageProcessResult.ReadFailed

    val finalBitmap = scaleAndRotate(sampledBitmap, orientation, maxDimension)
    val outStream = ByteArrayOutputStream()
    try {
        finalBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outStream)
        return ImageProcessResult.Success(outStream.toByteArray(), "image/jpeg")
    } finally {
        finalBitmap.recycle()
    }
}

/**
 * Reads, scales down, handles EXIF orientation, and compresses the selected image into JPEG.
 *
 * @param context Android context for content resolution.
 * @param uri Image source URI.
 * @param maxInputBytes Maximum allowable size for the input stream before compression.
 * @param maxDimension Target maximum width/height for downscaling.
 * @param quality JPEG compression quality factor (0..100).
 * @return Outcome status wrapping compressed JPEG bytes or failure reason.
 */
internal fun processAndCompressImage(
    context: Context,
    uri: Uri,
    maxInputBytes: Int = MAX_IMAGE_INPUT_BYTES,
    maxDimension: Int = MAX_IMAGE_DIMENSION,
    quality: Int = IMAGE_COMPRESSION_QUALITY,
): ImageProcessResult = runCatching {
    val (rawBytes, isTooLarge) = readImageBytesBounded(context, uri, maxInputBytes)
    when {
        isTooLarge -> ImageProcessResult.TooLarge
        rawBytes == null || rawBytes.isEmpty() -> ImageProcessResult.ReadFailed
        else -> compressRawBytes(rawBytes, maxDimension, quality)
    }
}.getOrElse { ImageProcessResult.ReadFailed }

/**
 * Result representing outcome of user image selection and decoding.
 */
internal sealed interface ImageProcessResult {
    data class Success(val bytes: ByteArray, val mimeType: String) : ImageProcessResult {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Success) return false
            return bytes.contentEquals(other.bytes) && mimeType == other.mimeType
        }

        override fun hashCode(): Int = 31 * bytes.contentHashCode() + mimeType.hashCode()
    }

    data object TooLarge : ImageProcessResult
    data object ReadFailed : ImageProcessResult
}
