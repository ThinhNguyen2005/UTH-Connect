package com.giathinh.uthconnect.presentation.navigation

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

private const val LASER_DURATION_MILLIS = 1100
private const val PULSE_DURATION_MILLIS = 1400

/**
 * Biểu tượng quét mã QR động (Animated QR Scanner) cho tab trung tâm của thanh điều hướng.
 * Tái hiện hiệu ứng như ảnh GIF sống động:
 * 1. Khung ngắm quét mã 4 góc vuông bo cong.
 * 2. Vạch laser quét lên xuống liên tục (Laser Scan Beam).
 * 3. Vòng tròn hào quang thở nhẹ nhàng (Pulsing Halo) thu hút tương tác của sinh viên.
 */
@Composable
fun AnimatedQrScannerIcon(
    isSelected: Boolean,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "QrScannerMotion")

    val laserProgress by transition.animateFloat(
        initialValue = 0.18f,
        targetValue = 0.82f,
        animationSpec = infiniteRepeatable(
            animation = tween(LASER_DURATION_MILLIS, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "LaserProgress",
    )

    val pulseScale by transition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(PULSE_DURATION_MILLIS, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "PulseScale",
    )

    val pulseAlpha by transition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(PULSE_DURATION_MILLIS, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "PulseAlpha",
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val laserColor = MaterialTheme.colorScheme.tertiary

    Box(
        modifier = modifier.size(40.dp),
        contentAlignment = Alignment.Center,
    ) {
        // Vòng hào quang phát sáng thở (Breathing Halo)
        Box(
            modifier = Modifier
                .size(38.dp)
                .scale(if (isSelected) pulseScale else 1f)
                .clip(CircleShape)
                .background(primaryColor.copy(alpha = if (isSelected) pulseAlpha else 0.12f)),
        )

        // Nút tròn nền
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(if (isSelected) primaryColor else MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.size(20.dp)) {
                val strokeWidth = 2.dp.toPx()
                val cornerLen = 4.5.dp.toPx()
                val w = size.width
                val h = size.height
                val frameColor = if (isSelected) Color.White else primaryColor

                // 1. Góc trên - trái
                drawLine(frameColor, Offset(0f, 0f), Offset(cornerLen, 0f), strokeWidth, StrokeCap.Round)
                drawLine(frameColor, Offset(0f, 0f), Offset(0f, cornerLen), strokeWidth, StrokeCap.Round)

                // 2. Góc trên - phải
                drawLine(frameColor, Offset(w, 0f), Offset(w - cornerLen, 0f), strokeWidth, StrokeCap.Round)
                drawLine(frameColor, Offset(w, 0f), Offset(w, cornerLen), strokeWidth, StrokeCap.Round)

                // 3. Góc dưới - trái
                drawLine(frameColor, Offset(0f, h), Offset(cornerLen, h), strokeWidth, StrokeCap.Round)
                drawLine(frameColor, Offset(0f, h), Offset(0f, h - cornerLen), strokeWidth, StrokeCap.Round)

                // 4. Góc dưới - phải
                drawLine(frameColor, Offset(w, h), Offset(w - cornerLen, h), strokeWidth, StrokeCap.Round)
                drawLine(frameColor, Offset(w, h), Offset(w, h - cornerLen), strokeWidth, StrokeCap.Round)

                // 3 ô vuông nhỏ nhận diện QR ở 3 góc
                val boxSize = 2.8.dp.toPx()
                val pad = 2.2.dp.toPx()
                drawRoundRect(frameColor, Offset(pad, pad), Size(boxSize, boxSize), CornerRadius(1f, 1f))
                drawRoundRect(frameColor, Offset(w - pad - boxSize, pad), Size(boxSize, boxSize), CornerRadius(1f, 1f))
                drawRoundRect(frameColor, Offset(pad, h - pad - boxSize), Size(boxSize, boxSize), CornerRadius(1f, 1f))

                // Tia laser quét động lên xuống
                val laserY = h * laserProgress
                drawLine(
                    color = if (isSelected) Color.White else laserColor,
                    start = Offset(2.dp.toPx(), laserY),
                    end = Offset(w - 2.dp.toPx(), laserY),
                    strokeWidth = 1.8.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            }
        }
    }
}
