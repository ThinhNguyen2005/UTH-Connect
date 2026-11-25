package com.giathinh.uthconnect.presentation.navigation

import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.giathinh.uthconnect.core.designsystem.theme.UthDimens

// ---- Dimension & Timing Tokens (Tránh MagicNumber) -------------------------
private const val GLYPH_SIZE_DP = 20
private const val CONTAINER_RADIUS_DP = 10
private const val BORDER_WIDTH_DP = 1

private const val SWEEP_DURATION_MS = 480
private const val FADE_DURATION_MS = 240

private const val ACTIVE_SCALE = 1.0f
private const val INACTIVE_SCALE = 0.94f

private const val INACTIVE_CONTAINER_ALPHA = 0.45f
private const val INACTIVE_BORDER_ALPHA = 0.50f

private const val LASER_START_Y_RATIO = 0.15f
private const val LASER_END_Y_RATIO = 0.85f

private const val CORNER_STROKE_DP = 1.8f
private const val CORNER_LEN_RATIO = 0.28f

private const val FINDER_BOX_RATIO = 0.28f
private const val FINDER_PAD_RATIO = 0.08f
private const val FINDER_RADIUS_DP = 1.2f
private const val FINDER_STROKE_DP = 1.2f
private const val FINDER_DOT_RATIO = 0.10f
private const val FINDER_DOT_RADIUS_DP = 0.6f
private const val DATA_DOT_RADIUS_DP = 1.2f

private const val LASER_STROKE_DP = 1.8f
private const val LASER_GLOW_HEIGHT_DP = 3.5f
private const val LASER_GLOW_ALPHA = 0.35f
private const val LASER_MARGIN_DP = 2f

@Composable
fun AnimatedQrScannerIcon(
    interactionSource: InteractionSource,
    modifier: Modifier = Modifier,
) {
    val reduceMotion = rememberReduceMotion()
    val isPressed by interactionSource.collectIsPressedAsState()

    // Animation co giãn phản hồi tương tác (Spring mượt mà)
    val scale by animateFloatAsState(
        targetValue = if (isPressed) ACTIVE_SCALE else INACTIVE_SCALE,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow,
        ),
        label = "QrScannerScale",
    )

    // The sweep runs while the scanner control is pressed.
    val laserProgress = remember { Animatable(LASER_START_Y_RATIO) }
    val laserAlpha = remember { Animatable(0f) }

    LaunchedEffect(isPressed, reduceMotion) {
        if (isPressed && !reduceMotion) {
            laserProgress.snapTo(LASER_START_Y_RATIO)
            laserAlpha.snapTo(1f)
            // Quét từ trên xuống dưới
            laserProgress.animateTo(
                targetValue = LASER_END_Y_RATIO,
                animationSpec = tween(SWEEP_DURATION_MS, easing = FastOutSlowInEasing),
            )
            // Mờ dần rồi kết thúc animation, không lặp vô tận
            laserAlpha.animateTo(
                targetValue = 0f,
                animationSpec = tween(FADE_DURATION_MS, easing = LinearOutSlowInEasing),
            )
        } else {
            laserAlpha.snapTo(0f)
            laserProgress.snapTo(LASER_START_Y_RATIO)
        }
    }

    val containerColor = if (isPressed) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = INACTIVE_CONTAINER_ALPHA)
    }

    val borderColor = if (isPressed) {
        Color.Transparent
    } else {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = INACTIVE_BORDER_ALPHA)
    }

    val glyphColor = if (isPressed) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    val laserColor = if (isPressed) {
        MaterialTheme.colorScheme.secondary
    } else {
        MaterialTheme.colorScheme.primary
    }

    val containerShape = remember { RoundedCornerShape(CONTAINER_RADIUS_DP.dp) }

    Box(
        modifier = modifier
            .size(UthDimens.Size.Icon)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(containerShape)
            .background(containerColor)
            .border(BORDER_WIDTH_DP.dp, borderColor, containerShape),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(GLYPH_SIZE_DP.dp)) {
            val w = size.width
            val h = size.height
            val strokeWidth = CORNER_STROKE_DP.dp.toPx()
            val cornerLen = w * CORNER_LEN_RATIO

            // 1. Khung ngắm 4 góc vuông bo cong
            drawLine(
                color = glyphColor,
                start = Offset(0f, 0f),
                end = Offset(cornerLen, 0f),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = glyphColor,
                start = Offset(0f, 0f),
                end = Offset(0f, cornerLen),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round,
            )

            drawLine(
                color = glyphColor,
                start = Offset(w, 0f),
                end = Offset(w - cornerLen, 0f),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = glyphColor,
                start = Offset(w, 0f),
                end = Offset(w, cornerLen),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round,
            )

            drawLine(
                color = glyphColor,
                start = Offset(0f, h),
                end = Offset(cornerLen, h),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = glyphColor,
                start = Offset(0f, h),
                end = Offset(0f, h - cornerLen),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round,
            )

            drawLine(
                color = glyphColor,
                start = Offset(w, h),
                end = Offset(w - cornerLen, h),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = glyphColor,
                start = Offset(w, h),
                end = Offset(w, h - cornerLen),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round,
            )

            // 2. Các khối Finder pattern (3 góc định vị)
            val boxSize = w * FINDER_BOX_RATIO
            val pad = w * FINDER_PAD_RATIO
            val boxRadius = CornerRadius(FINDER_RADIUS_DP.dp.toPx())
            val boxStroke = Stroke(width = FINDER_STROKE_DP.dp.toPx())
            val dotSize = w * FINDER_DOT_RATIO
            val dotRadius = CornerRadius(FINDER_DOT_RADIUS_DP.dp.toPx())
            val dotOffset = (boxSize - dotSize) / 2f

            // Góc trên - trái
            drawRoundRect(
                color = glyphColor,
                topLeft = Offset(pad, pad),
                size = Size(boxSize, boxSize),
                cornerRadius = boxRadius,
                style = boxStroke,
            )
            drawRoundRect(
                color = glyphColor,
                topLeft = Offset(pad + dotOffset, pad + dotOffset),
                size = Size(dotSize, dotSize),
                cornerRadius = dotRadius,
            )

            // Góc trên - phải
            val trX = w - pad - boxSize
            drawRoundRect(
                color = glyphColor,
                topLeft = Offset(trX, pad),
                size = Size(boxSize, boxSize),
                cornerRadius = boxRadius,
                style = boxStroke,
            )
            drawRoundRect(
                color = glyphColor,
                topLeft = Offset(trX + dotOffset, pad + dotOffset),
                size = Size(dotSize, dotSize),
                cornerRadius = dotRadius,
            )

            // Góc dưới - trái
            val blY = h - pad - boxSize
            drawRoundRect(
                color = glyphColor,
                topLeft = Offset(pad, blY),
                size = Size(boxSize, boxSize),
                cornerRadius = boxRadius,
                style = boxStroke,
            )
            drawRoundRect(
                color = glyphColor,
                topLeft = Offset(pad + dotOffset, blY + dotOffset),
                size = Size(dotSize, dotSize),
                cornerRadius = dotRadius,
            )

            // Điểm dữ liệu góc dưới - phải
            val brCenterX = w - pad - (boxSize / 2f)
            val brCenterY = h - pad - (boxSize / 2f)
            drawCircle(
                color = glyphColor,
                radius = DATA_DOT_RADIUS_DP.dp.toPx(),
                center = Offset(brCenterX, brCenterY),
            )

            // 3. Tia laser quét (chỉ vẽ khi alpha > 0f)
            val alpha = laserAlpha.value
            if (alpha > 0f) {
                val laserY = h * laserProgress.value
                val laserMargin = LASER_MARGIN_DP.dp.toPx()
                val glowHeight = LASER_GLOW_HEIGHT_DP.dp.toPx()

                // Dải hào quang mềm quanh tia laser
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            laserColor.copy(alpha = LASER_GLOW_ALPHA * alpha),
                            Color.Transparent,
                        ),
                        startY = laserY - glowHeight,
                        endY = laserY + glowHeight,
                    ),
                    topLeft = Offset(laserMargin, laserY - glowHeight),
                    size = Size(w - (laserMargin * 2f), glowHeight * 2f),
                )

                // Tia laser chính mờ dần ở hai cạnh
                drawLine(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            laserColor.copy(alpha = alpha),
                            laserColor.copy(alpha = alpha),
                            Color.Transparent,
                        ),
                        startX = laserMargin,
                        endX = w - laserMargin,
                    ),
                    start = Offset(laserMargin, laserY),
                    end = Offset(w - laserMargin, laserY),
                    strokeWidth = LASER_STROKE_DP.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            }
        }
    }
}

/**
 * Kiểm tra thiết lập hệ thống về giảm chuyển động (animator duration scale = 0).
 */
@Composable
private fun rememberReduceMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        val resolver = context.contentResolver
        Settings.Global.getFloat(
            resolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        ) == 0f
    }
}
