package com.giathinh.uthconnect.presentation.scanner

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.giathinh.uthconnect.R
import com.giathinh.uthconnect.core.designsystem.theme.UthDimens
import com.giathinh.uthconnect.domain.usecase.CheckInResult

@Composable
fun ScannerScreen(
    state: ScannerUiState,
    onAction: (ScannerAction) -> Unit,
    onBack: () -> Unit,
    onRequestCamera: () -> Unit = {},
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(UthDimens.Spacing.Xl),
        verticalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Lg),
    ) {
        TextButton(onClick = onBack) { Text(stringResource(R.string.scanner_back)) }
        Text(stringResource(R.string.scanner_title), style = MaterialTheme.typography.headlineSmall)
        if (!state.authorized) {
            Text(stringResource(R.string.scanner_forbidden))
        } else {
            Text(stringResource(R.string.scanner_count, state.successCount))
            when {
                !state.cameraPermissionGranted -> {
                    Text(stringResource(R.string.scanner_camera_denied))
                    Button(onClick = onRequestCamera) { Text(stringResource(R.string.scanner_camera_permission)) }
                }

                state.cameraError -> {
                    Text(stringResource(R.string.scanner_camera_error))
                    Button(onClick = { onAction(ScannerAction.ClearScan) }) {
                        Text(stringResource(R.string.scanner_camera_retry))
                    }
                }

                state.code.isNotBlank() -> {
                    Text(stringResource(R.string.scanner_camera_received))
                    TextButton(onClick = { onAction(ScannerAction.ClearScan) }) { Text(stringResource(R.string.scanner_scan_again)) }
                }

                else -> key(state.scanSession) {
                    ScannerCameraFrame(
                        onQrCode = { onAction(ScannerAction.CameraCodeDetected(it)) },
                        onError = { onAction(ScannerAction.CameraErrorChanged(true)) },
                    )
                    Text(stringResource(R.string.scanner_camera_scanning))
                }
            }
            OutlinedTextField(
                value = state.code,
                onValueChange = { onAction(ScannerAction.CodeChanged(it)) },
                label = { Text(stringResource(R.string.scanner_code)) },
                singleLine = true,
                enabled = !state.isSubmitting,
            )
            Button(onClick = { onAction(ScannerAction.Submit) }, enabled = !state.isSubmitting) {
                Text(stringResource(R.string.scanner_submit))
            }
            state.result?.let { result ->
                Text(
                    stringResource(
                        when (result) {
                            CheckInResult.SUCCESS -> R.string.scanner_success
                            CheckInResult.ALREADY_CHECKED_IN -> R.string.scanner_duplicate
                            CheckInResult.NOT_REGISTERED -> R.string.scanner_not_registered
                            CheckInResult.INVALID_CODE -> R.string.scanner_invalid
                            CheckInResult.FORBIDDEN -> R.string.scanner_forbidden
                            CheckInResult.ERROR -> R.string.scanner_error
                        },
                    ),
                )
            }
        }
    }
}

@Composable
private fun ScannerCameraFrame(
    onQrCode: (String) -> Unit,
    onError: () -> Unit,
) {
    val shade = MaterialTheme.colorScheme.scrim.copy(alpha = 0.62f)
    val cornerColor = MaterialTheme.colorScheme.onPrimary
    Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(MaterialTheme.shapes.medium)) {
        QrCameraPreview(Modifier.matchParentSize(), onQrCode, onError)
        Canvas(Modifier.matchParentSize()) {
            val side = minOf(size.width, size.height) * 0.72f
            val left = (size.width - side) / 2f
            val top = (size.height - side) / 2f
            val right = left + side
            val bottom = top + side
            drawRect(shade, size = Size(size.width, top))
            drawRect(shade, topLeft = Offset(0f, top), size = Size(left, side))
            drawRect(shade, topLeft = Offset(right, top), size = Size(size.width - right, side))
            drawRect(shade, topLeft = Offset(0f, bottom), size = Size(size.width, size.height - bottom))

            val corner = UthDimens.Spacing.Xxl.toPx()
            val stroke = UthDimens.Size.ProgressStroke.toPx()
            fun drawCorner(x: Float, y: Float, horizontal: Float, vertical: Float) {
                drawLine(cornerColor, Offset(x, y), Offset(x + horizontal * corner, y), stroke, StrokeCap.Round)
                drawLine(cornerColor, Offset(x, y), Offset(x, y + vertical * corner), stroke, StrokeCap.Round)
            }
            drawCorner(left, top, 1f, 1f)
            drawCorner(right, top, -1f, 1f)
            drawCorner(left, bottom, 1f, -1f)
            drawCorner(right, bottom, -1f, -1f)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ScannerPreview() {
    MaterialTheme { ScannerScreen(ScannerUiState(authorized = true), {}, {}) }
}
