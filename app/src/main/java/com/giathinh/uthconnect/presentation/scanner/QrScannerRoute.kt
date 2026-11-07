package com.giathinh.uthconnect.presentation.scanner

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.net.Uri
import android.provider.Settings
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.giathinh.uthconnect.domain.qr.QrFrameDecoder
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

/**
 * Máy quét QR điểm danh chạy trong app, thay cho luồng cũ mở app Camera hệ thống rồi
 * chụp một tấm JPEG: CameraX đẩy frame `YUV_420_888` liên tục, [QrFrameDecoder] giải
 * mã ngay trên mặt phẳng Y, kết quả được gửi thẳng xuống [onToken].
 *
 * Route là nơi duy nhất được đụng Context/quyền/CameraX theo quy ước presentation;
 * [QrScannerScreen] chỉ nhận [QrScannerUiState] và phát [QrScannerAction].
 *
 * Chống quét trùng: một mã đã gửi thì không gửi lại trong cùng phiên, kể cả khi
 * người dùng vẫn chĩa vé vào camera (server sẽ trả `ALREADY_CHECKED_IN`, nhưng ép
 * người quét bấm "Quét tiếp" chủ ý hơn). Sau lượt thất bại mã được cho phép gửi lại.
 */
@Composable
fun QrScannerDialog(
    busy: Boolean,
    resultMessage: String?,
    errorMessage: String?,
    checkedInCount: Int,
    onDismiss: () -> Unit,
    onContinue: () -> Unit,
    onToken: (String) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val haptics = LocalHapticFeedback.current

    var granted by remember {
        val allowed = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
        mutableStateOf(allowed == PackageManager.PERMISSION_GRANTED)
    }
    var permissionAsked by remember { mutableStateOf(granted) }
    var cameraError by remember { mutableStateOf<String?>(null) }
    var bindAttempt by remember { mutableIntStateOf(0) }
    var torchOn by remember { mutableStateOf(false) }
    var torchAvailable by remember { mutableStateOf(false) }
    var pendingToken by remember { mutableStateOf<String?>(null) }
    var lastToken by remember { mutableStateOf<String?>(null) }
    var camera by remember { mutableStateOf<Camera?>(null) }

    val outcome = when {
        errorMessage != null -> QrScannerOutcome.Failure
        resultMessage != null -> QrScannerOutcome.Success
        busy -> QrScannerOutcome.Processing
        else -> QrScannerOutcome.Idle
    }
    val state = QrScannerUiState(
        permissionAsked = permissionAsked,
        permissionGranted = granted,
        cameraError = cameraError,
        outcome = outcome,
        resultText = errorMessage ?: resultMessage,
        checkedInCount = checkedInCount,
        torchOn = torchOn,
        torchAvailable = torchAvailable,
    )

    val requestPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { allowed ->
        granted = allowed
        permissionAsked = true
    }
    val openPermissionSettings = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { }

    // Người dùng có thể cấp quyền trong Cài đặt thay vì hộp thoại; quay lại phải đọc lại
    // trạng thái thật, không thì màn quét vẫn báo chưa có quyền.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val allowed = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                if (allowed == PackageManager.PERMISSION_GRANTED) {
                    granted = true
                    permissionAsked = true
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Bíp xác nhận/ từ chối bằng ToneGenerator: không cần file âm thanh, không xin quyền
    // audio; một số máy chặn nên mọi lời gọi đều bọc runCatching ở dưới.
    val tones = remember {
        runCatching { ToneGenerator(AudioManager.STREAM_MUSIC, ToneGenerator.MAX_VOLUME) }.getOrNull()
    }
    DisposableEffect(tones) { onDispose { tones?.release() } }

    val handle: (QrScannerAction) -> Unit = { action ->
        when (action) {
            QrScannerAction.Dismiss -> onDismiss()

            QrScannerAction.RequestPermission -> requestPermission.launch(Manifest.permission.CAMERA)

            QrScannerAction.OpenPermissionSettings -> {
                val intent = Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", context.packageName, null),
                )
                openPermissionSettings.launch(intent)
            }

            QrScannerAction.RetryCamera -> {
                cameraError = null
                bindAttempt += 1
            }

            QrScannerAction.ToggleTorch -> torchOn = !torchOn

            QrScannerAction.ContinueScanning -> {
                pendingToken = null
                // Mã vừa bị từ chối thì cho phép gửi lại chính nó (BTC sửa hậu quả rồi quét
                // tiếp); mã đã thành công thì giữ để không vô tình điểm danh lần hai.
                if (outcome == QrScannerOutcome.Failure) lastToken = null
                onContinue()
            }
        }
    }

    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    LaunchedEffect(cameraProviderFuture) {
        cameraProviderFuture.addListener(
            { cameraProvider = runCatching { cameraProviderFuture.get() }.getOrNull() },
            ContextCompat.getMainExecutor(context),
        )
    }

    val previewView = remember {
        PreviewView(context).apply {
            // COMPATIBLE = TextureView. Với SurfaceView (mặc định) hình camera được vẽ ở
            // layer riêng và "đục thủng" mọi overlay Compose, khung ngắm sẽ biến mất.
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
    DisposableEffect(analysisExecutor) { onDispose { analysisExecutor.shutdown() } }

    LaunchedEffect(cameraProvider, granted, bindAttempt) {
        val provider = cameraProvider ?: return@LaunchedEffect
        if (!granted) return@LaunchedEffect

        val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
        val analysis = ImageAnalysis.Builder()
            .setResolutionSelector(
                ResolutionSelector.Builder()
                    .setResolutionStrategy(
                        ResolutionStrategy(
                            ANALYSIS_TARGET_SIZE,
                            ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER,
                        ),
                    )
                    .build(),
            )
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
            .build()
        analysis.setAnalyzer(analysisExecutor) { frame ->
            val token = runCatching { decodeFrame(frame) }.getOrNull()
            frame.close()
            if (token != null) lifecycleOwner.lifecycleScope.launch { pendingToken = token }
        }

        cameraError = null
        try {
            provider.unbindAll()
            camera = provider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                analysis,
            )
            torchAvailable = camera?.cameraInfo?.hasFlashUnit() == true
        } catch (error: Exception) {
            analysis.clearAnalyzer()
            camera = null
            cameraError = error.localizedMessage ?: error.javaClass.simpleName
        }

        try {
            // Giữ camera chạy tới khi dialog đóng hoặc quyền/bind đổi; finally là nơi
            // duy nhất nhả camera, để EventsScreen không bao giờ giữ hai phiên cùng lúc.
            awaitCancellation()
        } finally {
            analysis.clearAnalyzer()
            runCatching { provider.unbindAll() }
            camera = null
        }
    }

    LaunchedEffect(camera, torchOn) { camera?.cameraControl?.enableTorch(torchOn) }

    LaunchedEffect(pendingToken, outcome, granted) {
        val token = pendingToken ?: return@LaunchedEffect
        pendingToken = null
        if (!granted || outcome != QrScannerOutcome.Idle) return@LaunchedEffect
        if (token == lastToken) return@LaunchedEffect
        lastToken = token
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        runCatching { tones?.startTone(ToneGenerator.TONE_PROP_BEEP, TONE_DURATION_MS) }
        onToken(token)
    }

    LaunchedEffect(outcome) {
        when (outcome) {
            QrScannerOutcome.Success -> {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                runCatching { tones?.startTone(ToneGenerator.TONE_PROP_ACK, TONE_DURATION_MS) }
            }

            QrScannerOutcome.Failure -> {
                haptics.performHapticFeedback(HapticFeedbackType.VirtualKey)
                runCatching { tones?.startTone(ToneGenerator.TONE_PROP_NACK, TONE_DURATION_MS) }
            }

            else -> Unit
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
            QrScannerScreen(
                state = state,
                onAction = handle,
                cameraPreview = { previewModifier ->
                    AndroidView(factory = { previewView }, modifier = previewModifier)
                },
            )
        }
    }
}

/**
 * Lấy mặt phẳng Y của một frame và giải mã. Không xoay, không copy sang Bitmap:
 * [QrFrameDecoder] ăn thẳng luminance nên một frame 640×480 chỉ tốn vài ms.
 */
private fun decodeFrame(frame: ImageProxy): String? {
    val plane = frame.planes.firstOrNull() ?: return null
    val width = frame.width
    val height = frame.height
    val stride = plane.rowStride
    if (width <= 0 || height <= 0 || stride < width) return null
    if (plane.buffer.remaining() < (height - 1) * stride + width) return null
    val luminance = QrFrameDecoder.compactLuminance(plane.buffer, width, height, stride)
    return QrFrameDecoder.decode(luminance, width, height)
}

/**
 * Cỡ khung hình đưa vào giải mã. Đây là thông số cảm biến, không phải kích thước
 * thiết kế nên không thuộc `UthDimens`; đủ nhỏ để ZXing chạy kịp 30 fps trên máy
 * tầm trung và đủ lớn để đọc vé in trên giấy A5 từ khoảng cách cả sải tay.
 */
private val ANALYSIS_TARGET_SIZE = Size(640, 480)

/** Bíp đủ ngắn để không chồng lên bíp của lượt quét kế tiếp khi BTC quét liên tục. */
private const val TONE_DURATION_MS = 150
