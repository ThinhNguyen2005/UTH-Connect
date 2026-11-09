package com.giathinh.uthconnect.presentation.utilities.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.giathinh.uthconnect.R
import com.giathinh.uthconnect.core.designsystem.component.UthCard
import com.giathinh.uthconnect.core.designsystem.component.UthSectionHeader
import com.giathinh.uthconnect.core.designsystem.theme.UthDimens
import com.giathinh.uthconnect.core.designsystem.theme.UthPreview
import com.giathinh.uthconnect.core.designsystem.theme.UthTheme
import com.giathinh.uthconnect.domain.model.LANGUAGE_TAG_ENGLISH
import com.giathinh.uthconnect.domain.model.LANGUAGE_TAG_VIETNAMESE
import com.giathinh.uthconnect.domain.model.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    uiState: SettingsUiState = SettingsUiState(),
    onToggleTuitionReminder: (Boolean) -> Unit = {},
    onToggleTrainingPointNotif: (Boolean) -> Unit = {},
    onSelectLanguage: (String) -> Unit = {},
    onSelectThemeMode: (ThemeMode) -> Unit = {},
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.common_back),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
        modifier = modifier.fillMaxSize(),
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = UthDimens.Spacing.Xl),
        ) {
            Spacer(Modifier.height(UthDimens.Spacing.Md))

            // Nhóm Cài đặt Thông báo
            UthSectionHeader(title = stringResource(R.string.settings_notification_header))

            UthCard(modifier = Modifier.fillMaxWidth()) {
                // Toggle Nhắc nhở hạn nộp học phí
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.settings_notif_tuition),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(Modifier.height(UthDimens.Spacing.Xs))
                        Text(
                            text = stringResource(R.string.settings_notif_tuition_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = UthTheme.extendedColors.textSecondary,
                        )
                    }
                    Switch(
                        checked = uiState.tuitionReminderEnabled,
                        onCheckedChange = onToggleTuitionReminder,
                    )
                }

                Spacer(Modifier.height(UthDimens.Spacing.Md))
                HorizontalDivider(color = DividerDefaults.color.copy(alpha = UthDimens.Opacity.SubtleDivider))
                Spacer(Modifier.height(UthDimens.Spacing.Md))

                // Toggle Thông báo điểm rèn luyện
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.settings_notif_training),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(Modifier.height(UthDimens.Spacing.Xs))
                        Text(
                            text = stringResource(R.string.settings_notif_training_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = UthTheme.extendedColors.textSecondary,
                        )
                    }
                    Switch(
                        checked = uiState.trainingPointNotifEnabled,
                        onCheckedChange = onToggleTrainingPointNotif,
                    )
                }
            }

            // Nhóm Ngôn ngữ & Giao diện: chọn là áp ngay, người dùng không phải thoát app.
            UthSectionHeader(title = stringResource(R.string.settings_appearance_header))

            UthCard(modifier = Modifier.fillMaxWidth()) {
                SettingsChoiceRow(
                    title = stringResource(R.string.settings_language_title),
                    description = stringResource(R.string.settings_language_desc),
                    options = listOf(
                        stringResource(R.string.settings_language_vi),
                        stringResource(R.string.settings_language_en),
                    ),
                    selectedIndex = selectedLanguageIndex(uiState.languageTag),
                    onSelectOption = { index -> onSelectLanguage(languageTagAt(index)) },
                )

                Spacer(Modifier.height(UthDimens.Spacing.Md))
                HorizontalDivider(color = DividerDefaults.color.copy(alpha = UthDimens.Opacity.SubtleDivider))
                Spacer(Modifier.height(UthDimens.Spacing.Md))

                SettingsChoiceRow(
                    title = stringResource(R.string.settings_theme_title),
                    description = stringResource(R.string.settings_theme_desc),
                    options = listOf(
                        stringResource(R.string.settings_theme_system),
                        stringResource(R.string.settings_theme_light),
                        stringResource(R.string.settings_theme_dark),
                    ),
                    // ThemeMode được khai báo đúng thứ tự Hệ thống → Sáng → Tối nên
                    // ordinal chính là vị trí nút, không cần bảng tra.
                    selectedIndex = uiState.themeMode.ordinal,
                    onSelectOption = { index -> onSelectThemeMode(ThemeMode.entries[index]) },
                )
            }

            // Nhóm Thông tin ứng dụng
            UthSectionHeader(title = stringResource(R.string.settings_app_info_header))

            UthCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = UthDimens.Spacing.Xl),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.settings_version_label),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = uiState.appVersion,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

                Spacer(Modifier.height(UthDimens.Spacing.Md))
                HorizontalDivider(color = DividerDefaults.color.copy(alpha = UthDimens.Opacity.SubtleDivider))
                Spacer(Modifier.height(UthDimens.Spacing.Md))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.settings_team_label),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = uiState.developer,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

/**
 * Hàng lựa chọn dạng segmented trong Cài đặt: tiêu đề + mô tả bên trái, cụm nút chọn
 * bên dưới — cùng kiểu trình bày với hàng toggle ở trên để một màn chỉ có một nhịp.
 */
@Composable
private fun SettingsChoiceRow(
    title: String,
    description: String,
    options: List<String>,
    selectedIndex: Int,
    onSelectOption: (Int) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(UthDimens.Spacing.Xs))
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = UthTheme.extendedColors.textSecondary,
        )
        Spacer(Modifier.height(UthDimens.Spacing.Sm))
        Row(horizontalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Xs)) {
            options.forEachIndexed { index, label ->
                SettingsChoiceButton(
                    label = label,
                    isSelected = index == selectedIndex,
                    onClick = { onSelectOption(index) },
                )
            }
        }
    }
}

/**
 * Nút chọn: cao [UthDimens.Size.SegmentedControlHeight] (= [UthDimens.Size.MinTouchTarget]),
 * nền `primaryContainer` + **dấu tích** khi đang chọn. Icon bắt buộc vì trạng thái không
 * được truyền tải chỉ bằng màu (`docs/UI-UX.md`).
 */
@Composable
private fun SettingsChoiceButton(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        modifier = Modifier.semantics { selected = isSelected },
        shape = MaterialTheme.shapes.small,
        color = if (isSelected) colors.primaryContainer else UthTheme.extendedColors.surfaceSubtle,
        contentColor = if (isSelected) colors.onPrimaryContainer else colors.onSurfaceVariant,
        border = BorderStroke(
            width = UthDimens.Size.Stroke,
            color = if (isSelected) colors.primary else colors.outlineVariant,
        ),
    ) {
        Row(
            modifier = Modifier
                .widthIn(min = UthDimens.Size.MinTouchTarget)
                .height(UthDimens.Size.SegmentedControlHeight)
                .padding(horizontal = UthDimens.Spacing.Md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Xs),
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    modifier = Modifier.size(UthDimens.Size.IconBadge),
                )
            }
            Text(text = label, style = MaterialTheme.typography.labelLarge)
        }
    }
}

/** Vị trí nút Tiếng Việt / Tiếng Anh trong hàng Ngôn ngữ. */
private const val INDEX_VIETNAMESE = 0
private const val INDEX_ENGLISH = 1

/**
 * Bảng tra hai chiều giữa vị trí nút và mã BCP-47: `SettingsChoiceRow` chỉ nói chuyện
 * bằng index nên phần tử ngữ nghĩa nằm ở đây, không rải trong Composable.
 */
private fun languageTagAt(index: Int): String = when (index) {
    INDEX_ENGLISH -> LANGUAGE_TAG_ENGLISH
    else -> LANGUAGE_TAG_VIETNAMESE
}

private fun selectedLanguageIndex(languageTag: String): Int = when (languageTag) {
    LANGUAGE_TAG_ENGLISH -> INDEX_ENGLISH
    else -> INDEX_VIETNAMESE
}

@Preview(name = "SettingsScreen Light", showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    UthPreview {
        SettingsScreen(onBack = {})
    }
}

@Preview(name = "SettingsScreen Dark", showBackground = true)
@Composable
private fun SettingsScreenDarkPreview() {
    UthPreview(darkTheme = true) {
        SettingsScreen(onBack = {})
    }
}
