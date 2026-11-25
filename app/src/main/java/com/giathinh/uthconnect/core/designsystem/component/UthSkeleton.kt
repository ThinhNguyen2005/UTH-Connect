package com.giathinh.uthconnect.core.designsystem.component

import android.animation.ValueAnimator
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.progressSemantics
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import com.giathinh.uthconnect.core.designsystem.theme.UthDimens
import com.giathinh.uthconnect.core.designsystem.theme.UthTheme

@Composable
internal fun rememberUthSkeletonProgress(): State<Float>? {
    if (!ValueAnimator.areAnimatorsEnabled()) return null
    return rememberInfiniteTransition(label = "skeleton").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(UthDimens.Motion.ShimmerMillis, easing = LinearEasing)),
        label = "skeletonProgress",
    )
}

@Composable
internal fun UthContentSkeleton(modifier: Modifier = Modifier, detail: Boolean = false) {
    val progress = rememberUthSkeletonProgress()
    Column(
        modifier = modifier.fillMaxWidth().progressSemantics().padding(UthDimens.Spacing.Lg),
        verticalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Md),
    ) {
        if (detail) {
            UthSkeletonBlock(
                progress,
                Modifier.fillMaxWidth(UthDimens.Layout.HomeSkeletonTitleFraction).height(UthDimens.Spacing.Xl),
            )
        }
        repeat(UthDimens.Layout.HomeSkeletonRowCount) {
            UthSkeletonBlock(progress, Modifier.fillMaxWidth().height(UthDimens.Size.ButtonHeight))
        }
    }
}

@Composable
internal fun UthSkeletonBlock(
    progress: State<Float>?,
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.medium,
) {
    val base = UthTheme.extendedColors.surfaceSubtle
    val highlight = MaterialTheme.colorScheme.surface.copy(alpha = UthDimens.Opacity.ShimmerHighlight)

    Box(
        modifier = modifier
            .clip(shape)
            .drawBehind {
                drawRect(color = base)
                val phase = progress?.value
                if (phase != null && size.width > 0f) {
                    val band = size.width * UthDimens.Layout.ShimmerBandFraction
                    val travel = size.width + band * 2
                    val start = -band + travel * phase
                    drawRect(
                        brush = Brush.linearGradient(
                            colors = listOf(Color.Transparent, highlight, Color.Transparent),
                            start = Offset(start, 0f),
                            end = Offset(start + band, size.height),
                        ),
                    )
                }
            },
    )
}
