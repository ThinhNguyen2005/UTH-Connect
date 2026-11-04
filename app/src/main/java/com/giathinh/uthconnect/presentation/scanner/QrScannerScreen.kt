package com.giathinh.uthconnect.presentation.scanner

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.giathinh.uthconnect.R
import com.giathinh.uthconnect.core.designsystem.theme.UthDimens
import com.giathinh.uthconnect.core.designsystem.theme.UthPreview

/**
 * Máy quét QR điểm danh chạy trực tiếp trong app: frame của camera được [Route]
 * đưa vào qua slot [cameraPreview], màn hình này chỉ vẽ lớp phủ căn khung, vạch
 * quét, bảng kết quả và trạng thái quyền.
 *
 * Screen không đụng ViewModel/Context theo quy ước presentation: toàn bộ side effect
 * (CameraX, quyền, beep, gọi RPC) nằm trong [QrScannerRoute].
 */
@Composable
fun QrScannerScreen(
    state: QrScannerUiState,
    onAction: (QrScannerAction) -> Unit,
    cameraPreview: @Composable (Modifier) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        if (state.permissionGranted && state.cameraError == null) {
            cameraPreview(Modifier.fillMaxSize())
            ScannerViewfinder(active = state.scanning)
            ScannerResultBanner(
                state = state,
                onContinue = { onAction(QrScannerAction.ContinueScanning) },
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
        ScannerTopBar(
            state = state,
            onDismiss = { onAction(QrScannerAction.Dismiss) },
            onToggleTorch = { onAction(QrScannerAction.ToggleTorch) },
            modifier = Modifier.align(Alignment.TopCenter),
        )
        when {
            !state.permissionGranted -> ScannerPermissionPanel(
                blocked = state.permissionBlocked,
                onAction = onAction,
            )

            state.cameraError != null -> ScannerCameraErrorPanel(
                message = state.cameraError.orEmpty(),
                onRetry = { onAction(QrScannerAction.RetryCamera) },
                onDismiss = { onAction(QrScannerAction.Dismiss) },
            )

            else -> Unit
        }
    }
}

/**
 * Lớp phủ nhân tạo đè lên hình camera thật: bốn dải làm tối chung quanh, bốn chữ L
 * định vị ([UthDimens.Size.QrDesignator]) và một vạch quét chạy trong khung khi
 * [active]. Vạch không phải hướng dẫn bắt buộc, chỉ giúp người quét biết máy còn sống.
 */
@Composable
private fun ScannerViewfinder(active: Boolean, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val sweep = remember { Animatable(0f) }
    if (active) {
        LaunchedEffect(Unit) {
            while (true) {
                sweep.snapTo(0f)
                sweep.animateTo(1f, tween(UthDimens.Motion.ScannerSweepMillis, easing = LinearEasing))
            }
        }
    }
    val side = UthDimens.Size.ScannerFrame
    val stroke = UthDimens.Size.ScannerStroke
    val arm = UthDimens.Size.QrDesignator
    Box(
        modifier = modifier
            .fillMaxSize()
            .drawBehind {
                val frame = side.toPx()
                val inset = ((size.width - frame) / 2f).coerceAtLeast(0f)
                val top = ((size.height - frame) / 2f).coerceAtLeast(0f)
                val scrim = Color.Black.copy(alpha = UthDimens.Opacity.ScannerScrim)
                drawRect(scrim, size = Size(size.width, top))
                drawRect(scrim, topLeft = Offset(0f, top + frame), size = Size(size.width, size.height - top - frame))
                drawRect(scrim, topLeft = Offset(0f, top), size = Size(inset, frame))
                drawRect(
                    scrim,
                    topLeft = Offset(inset + frame, top),
                    size = Size(size.width - inset - frame, frame),
                )
                drawViewfinderCorners(Offset(inset, top), frame, arm.toPx(), stroke.toPx(), colors.primary)
                if (active) {
                    val margin = stroke.toPx() * 2f
                    val y = top + margin + (frame - margin * 2f) * sweep.value
                    drawLine(
                        color = colors.primary,
                        start = Offset(inset + margin, y),
                        end = Offset(inset + frame - margin, y),
                        strokeWidth = stroke.toPx(),
                        cap = StrokeCap.Round,
                    )
                }
            },
    )
}

private fun DrawScope.drawViewfinderCorners(
    origin: Offset,
    side: Float,
    arm: Float,
    stroke: Float,
    color: Color,
) {
    val right = origin.x + side
    val bottom = origin.y + side
    val topLeft = origin
    val topRight = Offset(right, origin.y)
    val bottomLeft = Offset(origin.x, bottom)
    val bottomRight = Offset(right, bottom)
    drawLine(color, topLeft, Offset(origin.x + arm, origin.y), strokeWidth = stroke, cap = StrokeCap.Round)
    drawLine(color, topLeft, Offset(origin.x, origin.y + arm), strokeWidth = stroke, cap = StrokeCap.Round)
    drawLine(color, topRight, Offset(right - arm, origin.y), strokeWidth = stroke, cap = StrokeCap.Round)
    drawLine(color, topRight, Offset(right, origin.y + arm), strokeWidth = stroke, cap = StrokeCap.Round)
    drawLine(color, bottomLeft, Offset(origin.x + arm, bottom), strokeWidth = stroke, cap = StrokeCap.Round)
    drawLine(color, bottomLeft, Offset(origin.x, bottom - arm), strokeWidth = stroke, cap = StrokeCap.Round)
    drawLine(color, bottomRight, Offset(right - arm, bottom), strokeWidth = stroke, cap = StrokeCap.Round)
    drawLine(color, bottomRight, Offset(right, bottom - arm), strokeWidth = stroke, cap = StrokeCap.Round)
}

/** Thanh trên cùng: đóng màn quét, tiêu đề, bộ đếm lượt và đèn pin khi camera có flash. */
@Composable
private fun ScannerTopBar(
    state: QrScannerUiState,
    onDismiss: () -> Unit,
    onToggleTorch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface.copy(alpha = UthDimens.Opacity.ScannerPanel),
    ) {
        Row(
            modifier = Modifier.padding(
                start = UthDimens.Spacing.Xs,
                end = UthDimens.Spacing.Xs,
                top = UthDimens.Spacing.Sm,
                bottom = UthDimens.Spacing.Sm,
            ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.scanner_close))
            }
            Spacer(Modifier.width(UthDimens.Spacing.Sm))
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.scanner_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = if (state.checkedInCount > 0) {
                        stringResource(R.string.scanner_count, state.checkedInCount)
                    } else {
                        stringResource(R.string.scanner_hint)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (state.torchAvailable) {
                IconButton(
                    onClick = onToggleTorch,
                    modifier = Modifier.size(UthDimens.Size.ScannerIconButton),
                ) {
                    Icon(
                        painter = painterResource(
                            if (state.torchOn) R.drawable.ic_scanner_torch_on else R.drawable.ic_scanner_torch_off,
                        ),
                        contentDescription = stringResource(R.string.scanner_torch),
                        tint = if (state.torchOn) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }
        }
    }
}

/**
 * Bảng kết quả sát đáy màn hình. Thành công/cách biệt bằng `secondaryContainer`,
 * thất bại dùng `errorContainer` — cùng cặp màu M3 với các badge trạng thái trong app.
 * Server là nguồn sự thật nên nhãn luôn lấy từ `EventsUiState`, màn này không tự đoán.
 */
@Composable
private fun ScannerResultBanner(
    state: QrScannerUiState,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val container = when (state.outcome) {
        QrScannerOutcome.Success -> colors.secondaryContainer
        QrScannerOutcome.Failure -> colors.errorContainer
        else -> colors.surface.copy(alpha = UthDimens.Opacity.ScannerPanel)
    }
    val onContainer = when (state.outcome) {
        QrScannerOutcome.Success -> colors.onSecondaryContainer
        QrScannerOutcome.Failure -> colors.onErrorContainer
        else -> colors.onSurface
    }
    val label = when (state.outcome) {
        QrScannerOutcome.Success -> stringResource(R.string.scanner_state_success)
        QrScannerOutcome.Failure -> stringResource(R.string.scanner_state_failure)
        QrScannerOutcome.Processing -> stringResource(R.string.scanner_state_processing)
        QrScannerOutcome.Idle -> stringResource(R.string.scanner_state_idle)
    }
    Surface(
        modifier = modifier.fillMaxWidth().padding(UthDimens.Spacing.Lg),
        shape = UthDimens.Radius.LgShape,
        color = container,
    ) {
        Row(
            modifier = Modifier.padding(UthDimens.Spacing.Lg),
            horizontalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            when (state.outcome) {
                QrScannerOutcome.Processing -> CircularProgressIndicator(
                    modifier = Modifier.size(UthDimens.Size.ProgressInline),
                    strokeWidth = UthDimens.Size.ProgressInlineStroke,
                    color = onContainer,
                )

                QrScannerOutcome.Success -> Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = onContainer,
                    modifier = Modifier.size(UthDimens.Size.Icon),
                )

                else -> Unit
            }
            Column(Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.titleMedium, color = onContainer)
                state.resultText?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = onContainer)
                }
            }
            if (state.outcome == QrScannerOutcome.Success || state.outcome == QrScannerOutcome.Failure) {
                TextButton(onClick = onContinue) {
                    Text(stringResource(R.string.scanner_next), color = onContainer)
                }
            }
        }
    }
}

/** Toàn màn vì chưa có quyền thì camera chưa chạy, để nền đặc để chữ đủ tương phản. */
@Composable
private fun ScannerPermissionPanel(
    blocked: Boolean,
    onAction: (QrScannerAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Column(
            modifier = Modifier.fillMaxSize().padding(UthDimens.Spacing.Xl),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                stringResource(R.string.scanner_permission_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(UthDimens.Spacing.Lg))
            Text(
                text = stringResource(
                    if (blocked) R.string.scanner_permission_blocked else R.string.scanner_permission_body,
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(UthDimens.Spacing.Xxl))
            Button(
                onClick = {
                    onAction(if (blocked) QrScannerAction.OpenPermissionSettings else QrScannerAction.RequestPermission)
                },
                modifier = Modifier.fillMaxWidth().height(UthDimens.Size.ButtonHeight),
            ) {
                Text(
                    stringResource(
                        if (blocked) R.string.scanner_permission_settings else R.string.scanner_permission_grant,
                    ),
                )
            }
            TextButton(onClick = { onAction(QrScannerAction.Dismiss) }) { Text(stringResource(R.string.common_back)) }
        }
    }
}

/** Camera tồn tại nhưng từ chối bind (đang bị app khác giữ, thiếu camera sau…). */
@Composable
private fun ScannerCameraErrorPanel(
    message: String,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Column(
            modifier = Modifier.fillMaxSize().padding(UthDimens.Spacing.Xl),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                stringResource(R.string.scanner_camera_error, message),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(UthDimens.Spacing.Xxl))
            Button(
                onClick = onRetry,
                modifier = Modifier.fillMaxWidth().height(UthDimens.Size.ButtonHeight),
            ) {
                Text(stringResource(R.string.scanner_camera_retry))
            }
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_back)) }
        }
    }
}

@Composable
private fun ScannerPreview(state: QrScannerUiState) = UthPreview {
    QrScannerScreen(
        state = state,
        onAction = {},
        cameraPreview = { previewModifier ->
            Box(
                previewModifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            )
        },
    )
}

@Preview(name = "Scanner live light", showBackground = true)
@Composable
private fun ScannerLivePreview() = ScannerPreview(
    QrScannerUiState(permissionAsked = true, permissionGranted = true, torchAvailable = true),
)

@Preview(name = "Scanner success light", showBackground = true)
@Composable
private fun ScannerSuccessPreview() = ScannerPreview(
    QrScannerUiState(
        permissionAsked = true,
        permissionGranted = true,
        outcome = QrScannerOutcome.Success,
        resultText = "Điểm danh thành công",
        checkedInCount = 7,
        torchAvailable = true,
        torchOn = true,
    ),
)

@Preview(name = "Scanner permission light", showBackground = true)
@Composable
private fun ScannerPermissionPreview() = ScannerPreview(QrScannerUiState())
