package com.giathinh.uthconnect.core.designsystem.component

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.core.view.WindowCompat
import com.giathinh.uthconnect.R
import com.giathinh.uthconnect.core.designsystem.theme.UthDimens
import com.giathinh.uthconnect.core.designsystem.theme.UthTheme

/**
 * Card chuẩn theo `docs/Design-system.md` mục 4.2: nền `surface`, bo 16dp,
 * bóng 1dp + viền 1dp `outline`, padding trong 16dp.
 */
@Composable
fun UthCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    contentPadding: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = MaterialTheme.shapes.medium
    val border = BorderStroke(UthDimens.Size.Stroke, MaterialTheme.colorScheme.outline)
    val elevation = CardDefaults.cardElevation(defaultElevation = UthDimens.Elevation.Card)
    val colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    val cardContent: @Composable ColumnScope.() -> Unit = {
        Column(
            modifier = if (contentPadding) Modifier.padding(UthDimens.Spacing.Lg) else Modifier,
            content = content,
        )
    }

    if (onClick == null) {
        Card(
            modifier = modifier,
            shape = shape,
            border = border,
            elevation = elevation,
            colors = colors,
            content = cardContent,
        )
    } else {
        Card(
            onClick = onClick,
            modifier = modifier,
            shape = shape,
            border = border,
            elevation = elevation,
            colors = colors,
            content = cardContent,
        )
    }
}

/** Header của một nhóm nội dung: 14sp/600 + 40dp phía trước. */
@Composable
fun UthSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    action: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = UthDimens.Spacing.Section, bottom = UthDimens.Spacing.Sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        action?.invoke()
    }
}

/**
 * Nút chính cao 48dp, bo 8dp — `docs/Design-system.md` mục 4.1.
 * `loading = true` giữ nhãn, thông báo trạng thái và chặn chạm (chống double-submit).
 */
@Composable
fun UthPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    leadingIcon: ImageVector? = null,
) {
    val loadingDescription = stringResource(R.string.common_loading)
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        modifier = modifier.heightIn(min = UthDimens.Size.ButtonHeight).semantics {
            if (loading) stateDescription = loadingDescription
        },
        shape = MaterialTheme.shapes.small,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(UthDimens.Size.ProgressInline).clearAndSetSemantics { },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                strokeWidth = UthDimens.Size.ProgressInlineStroke,
            )
            Spacer(Modifier.width(UthDimens.Spacing.Sm))
            Text(text = text.uppercase(), style = MaterialTheme.typography.labelLarge)
        } else {
            if (leadingIcon != null) {
                Icon(leadingIcon, contentDescription = null, modifier = Modifier.size(UthDimens.Size.IconInline))
                Spacer(Modifier.width(UthDimens.Spacing.Sm))
            }
            Text(text = text.uppercase(), style = MaterialTheme.typography.labelLarge)
        }
    }
}

/**
 * Trạng thái rỗng / lỗi tải. Luôn kèm đúng một CTA khi người dùng còn lối thoát
 * (không dùng toast cho lỗi chặn luồng).
 */
@Composable
fun UthEmptyState(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val extended = UthTheme.extendedColors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(UthDimens.Spacing.Xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (icon != null) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = extended.textTertiary,
                    modifier = Modifier.size(UthDimens.Size.AvatarLarge),
                )
            }
            Spacer(Modifier.height(UthDimens.Spacing.Lg))
        }
        Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Spacer(Modifier.height(UthDimens.Spacing.Sm))
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = extended.textSecondary,
            textAlign = TextAlign.Center,
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(UthDimens.Spacing.Xl))
            UthPrimaryButton(
                text = actionLabel,
                onClick = onAction,
                leadingIcon = Icons.Filled.Refresh,
            )
        }
    }
}

/**
 * TopBar thương hiệu UTH dùng chung cho các màn chi tiết và chợ cộng đồng:
 * nền Navy (`primary`), vòng tròn ambient bị cắt gọn trong khung (`.clipToBounds()`),
 * tự padding thanh trạng thái và đảm bảo icon status bar sáng trên nền tối.
 */
@Composable
fun UthBrandTopBar(
    title: String,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trailingAction: (@Composable RowScope.() -> Unit)? = null,
) {
    val colors = MaterialTheme.colorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        DisposableEffect(view) {
            val window = view.context.findActivity()?.window
            val controller = window?.let { WindowCompat.getInsetsController(it, view) }
            val previousLightStatusBars = controller?.isAppearanceLightStatusBars
            controller?.isAppearanceLightStatusBars = false
            onDispose {
                if (previousLightStatusBars != null) {
                    controller.isAppearanceLightStatusBars = previousLightStatusBars
                }
            }
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clipToBounds()
            .background(colors.primary)
            .drawBehind {
                val ringRadius = size.width * UthDimens.Layout.HomeRingRadiusFraction
                val ringStroke = size.width * UthDimens.Layout.HomeRingStrokeFraction
                drawCircle(
                    color = colors.onPrimary.copy(alpha = UthDimens.Opacity.HomeHeaderRing),
                    radius = ringRadius,
                    center = Offset(size.width + ringRadius * UthDimens.Layout.HomeRingOffsetFraction, size.height),
                    style = Stroke(width = ringStroke),
                )
                drawCircle(
                    color = colors.secondary.copy(alpha = UthDimens.Opacity.HomeHeaderAccentRing),
                    radius = ringRadius * UthDimens.Layout.HomeAccentRingRadiusFactor,
                    center = Offset(size.width * UthDimens.Layout.HomeAccentRingCenterFraction, 0f),
                    style = Stroke(width = ringStroke * UthDimens.Layout.HomeAccentRingStrokeFactor),
                )
            }
            .statusBarsPadding()
            .padding(
                start = UthDimens.Spacing.Sm,
                end = UthDimens.Spacing.Lg,
                top = UthDimens.Spacing.Sm,
                bottom = UthDimens.Spacing.Sm,
            ),
    ) {
        IconButton(
            onClick = onNavigateBack,
            modifier = Modifier.size(UthDimens.Size.MinTouchTarget),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.nav_back),
                tint = colors.onPrimary,
            )
        }
        Spacer(Modifier.width(UthDimens.Spacing.Sm))
        Box(
            modifier = Modifier
                .size(UthDimens.Size.Avatar)
                .clip(MaterialTheme.shapes.small)
                .background(colors.onPrimary)
                .clearAndSetSemantics { },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.home_logo_abbr),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = colors.primary,
            )
        }
        Spacer(Modifier.width(UthDimens.Spacing.Md))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = colors.onPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.semantics { heading() },
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onPrimary.copy(alpha = UthDimens.Opacity.HeaderMetadata),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (trailingAction != null) {
            trailingAction()
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
