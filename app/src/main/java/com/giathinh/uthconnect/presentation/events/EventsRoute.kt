package com.giathinh.uthconnect.presentation.events

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.zxing.BinaryBitmap
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun EventsRoute(viewModel: EventsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pendingPhoto by remember { mutableStateOf<File?>(null) }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        val photo = pendingPhoto
        pendingPhoto = null
        if (photo != null) {
            scope.launch {
                try {
                    if (saved) {
                        val qr = withContext(Dispatchers.Default) { decodeTicketQr(photo) }
                        viewModel.checkIn(qr)
                    }
                } catch (_: Exception) {
                    viewModel.showError("Không đọc được QR. Đưa mã vào giữa khung camera và thử lại.")
                } finally {
                    photo.delete()
                }
            }
        }
    }
    EventsScreen(
        state = state,
        onRefresh = viewModel::reload,
        onRegister = viewModel::register,
        onCancel = viewModel::cancel,
        onSave = viewModel::saveEvent,
        onSetStatus = viewModel::setStatus,
        onScan = {
            runCatching {
                val directory = File(context.cacheDir, "checkin").apply { mkdirs() }
                val photo = File.createTempFile("qr-", ".jpg", directory)
                val uri: Uri = FileProvider.getUriForFile(
                    context, "${context.packageName}.checkin.fileprovider", photo,
                )
                pendingPhoto = photo
                camera.launch(uri)
            }.onFailure { error ->
                pendingPhoto?.delete()
                pendingPhoto = null
                viewModel.showError(error.message ?: "Không mở được camera")
            }
        },
    )
}

private fun decodeTicketQr(photo: File): String {
    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(photo.absolutePath, options)
    require(options.outWidth > 0 && options.outHeight > 0) { "Invalid image" }
    var sampleSize = 1
    while (maxOf(options.outWidth, options.outHeight) / sampleSize > 2048) sampleSize *= 2
    val bitmap = BitmapFactory.decodeFile(photo.absolutePath, BitmapFactory.Options().apply {
        inSampleSize = sampleSize
    }) ?: error("Cannot decode QR image")
    return try {
        decodeTicketQr(bitmap)
    } finally {
        bitmap.recycle()
    }
}

private fun decodeTicketQr(bitmap: Bitmap): String {
    val pixels = IntArray(bitmap.width * bitmap.height)
    bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
    val source = RGBLuminanceSource(bitmap.width, bitmap.height, pixels)
    return QRCodeReader().decode(BinaryBitmap(HybridBinarizer(source))).text
}
