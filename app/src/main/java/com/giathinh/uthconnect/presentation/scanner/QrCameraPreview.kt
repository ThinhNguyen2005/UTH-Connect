package com.giathinh.uthconnect.presentation.scanner

import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

@Composable
internal fun QrCameraPreview(
    modifier: Modifier = Modifier,
    onQrCode: (String) -> Unit,
    onError: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val latestOnQrCode by rememberUpdatedState(onQrCode)
    val latestOnError by rememberUpdatedState(onError)
    var previewView by remember { mutableStateOf<PreviewView?>(null) }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            PreviewView(ctx).also {
                it.implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                previewView = it
            }
        },
    )

    DisposableEffect(previewView, lifecycleOwner) {
        val view = previewView ?: return@DisposableEffect onDispose { }
        val mainExecutor = ContextCompat.getMainExecutor(context)
        val cameraExecutor = Executors.newSingleThreadExecutor()
        val disposed = AtomicBoolean(false)
        val processing = AtomicBoolean(false)
        val handled = AtomicBoolean(false)
        val activeProxy = AtomicReference<ImageProxy?>(null)
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()
        val scanner = BarcodeScanning.getClient(options)
        val providerFuture = ProcessCameraProvider.getInstance(context)
        var provider: ProcessCameraProvider? = null
        var preview: Preview? = null
        var analysis: ImageAnalysis? = null

        fun finishFrame(proxy: ImageProxy) {
            if (activeProxy.compareAndSet(proxy, null)) proxy.close()
            processing.set(false)
        }

        providerFuture.addListener({
            if (disposed.get()) return@addListener
            runCatching {
                val cameraProvider = providerFuture.get()
                if (disposed.get()) return@runCatching
                val cameraPreview = Preview.Builder().build().also {
                    it.surfaceProvider = view.surfaceProvider
                }
                val cameraAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                cameraAnalysis.setAnalyzer(cameraExecutor) { proxy ->
                    if (disposed.get() || handled.get() || !processing.compareAndSet(false, true)) {
                        proxy.close()
                        return@setAnalyzer
                    }
                    activeProxy.set(proxy)
                    try {
                        val mediaImage = proxy.image
                        if (mediaImage == null || disposed.get()) {
                            finishFrame(proxy)
                            return@setAnalyzer
                        }
                        val image = InputImage.fromMediaImage(mediaImage, proxy.imageInfo.rotationDegrees)
                        scanner.process(image)
                            .addOnSuccessListener(mainExecutor) { codes ->
                                if (!disposed.get()) {
                                    val value = codes.firstNotNullOfOrNull { it.rawValue }
                                    if (!value.isNullOrBlank() && handled.compareAndSet(false, true)) {
                                        latestOnQrCode(value)
                                    }
                                }
                            }
                            .addOnCompleteListener(mainExecutor) { finishFrame(proxy) }
                    } catch (_: Exception) {
                        finishFrame(proxy)
                        if (!disposed.get()) mainExecutor.execute { if (!disposed.get()) latestOnError() }
                    }
                }
                provider = cameraProvider
                preview = cameraPreview
                analysis = cameraAnalysis
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    cameraPreview,
                    cameraAnalysis,
                )
            }.onFailure { if (!disposed.get()) latestOnError() }
        }, mainExecutor)

        onDispose {
            disposed.set(true)
            analysis?.clearAnalyzer()
            val boundPreview = preview
            val boundAnalysis = analysis
            if (boundPreview != null && boundAnalysis != null) {
                runCatching { provider?.unbind(boundPreview, boundAnalysis) }
            }
            activeProxy.getAndSet(null)?.close()
            scanner.close()
            cameraExecutor.shutdown()
        }
    }
}
