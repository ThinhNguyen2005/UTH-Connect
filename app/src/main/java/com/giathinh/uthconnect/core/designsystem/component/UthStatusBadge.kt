package com.giathinh.uthconnect.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.giathinh.uthconnect.core.designsystem.theme.UthDimens
import com.giathinh.uthconnect.core.designsystem.theme.UthExtendedColors
import com.giathinh.uthconnect.core.designsystem.theme.UthTheme

/**
 * Tone trạng thái. Mỗi tone là một **cặp** nền + chữ đã đo tương phản
 * (`docs/Design-system.md` mục 1.4) — không tự phối tay.
 */
enum class BadgeTone { Success, Warning, Danger, Info, Accent, Neutral }

/**
 * Badge trạng thái cao ~24dp, bo góc `Sm` (4dp) và nền/chữ lấy theo [BadgeTone] — đúng mục 4.3
 * của `docs/Design-system.md`: badge là tag vuông mềm, không phải pill.
 *
 * Mặc định kèm icon để màu không phải tín hiệu duy nhất (người mù màu +
 * TalkBack). Truyền `icon = null` chỉ khi chữ đã tự mô tả, ví dụ "Gợi ý bởi AI".
 */
@Composable
fun UthStatusBadge(
    text: String,
    tone: BadgeTone,
    modifier: Modifier = Modifier,
    icon: ImageVector? = tone.defaultIcon(),
) {
    val (container, onContainer) = UthTheme.extendedColors.toneColors(tone)
    Row(
        modifier = modifier
            .background(container, MaterialTheme.shapes.extraSmall)
            .padding(horizontal = UthDimens.Spacing.Sm, vertical = UthDimens.Spacing.Xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = onContainer,
                modifier = Modifier.size(UthDimens.Size.IconBadge),
            )
            Spacer(Modifier.width(UthDimens.Spacing.Xs))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = onContainer,
        )
    }
}

private fun BadgeTone.defaultIcon(): ImageVector? = when (this) {
    BadgeTone.Success -> Icons.Filled.CheckCircle
    BadgeTone.Warning -> Icons.Filled.Warning
    BadgeTone.Danger -> Icons.Filled.Close
    BadgeTone.Info -> Icons.Filled.Info
    BadgeTone.Accent, BadgeTone.Neutral -> null
}

private fun UthExtendedColors.toneColors(tone: BadgeTone): Pair<Color, Color> = when (tone) {
    BadgeTone.Success -> successContainer to onSuccessContainer
    BadgeTone.Warning -> warningContainer to onWarningContainer
    BadgeTone.Danger -> dangerContainer to onDangerContainer
    BadgeTone.Info -> infoContainer to onInfoContainer
    BadgeTone.Accent -> accentContainer to onAccentContainer
    BadgeTone.Neutral -> surfaceSubtle to textSecondary
}
