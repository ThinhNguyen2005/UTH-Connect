package com.giathinh.uthconnect.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
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
    val clickModifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    Card(
        modifier = modifier.then(clickModifier),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(UthDimens.Size.Stroke, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = UthDimens.Elevation.Card),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = if (contentPadding) Modifier.padding(UthDimens.Spacing.Lg) else Modifier,
            content = content,
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
 * `loading = true` đổi chữ thành spinner và chặn chạm (chống double-submit).
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
    val extended = UthTheme.extendedColors
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        modifier = modifier.height(UthDimens.Size.ButtonHeight),
        shape = MaterialTheme.shapes.small,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            disabledContentColor = extended.textTertiary,
        ),
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(UthDimens.Size.ProgressInline),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = UthDimens.Size.ProgressInlineStroke,
            )
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
