package com.giathinh.uthconnect.presentation.utilities.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.giathinh.uthconnect.R
import com.giathinh.uthconnect.core.designsystem.component.UthCard
import com.giathinh.uthconnect.core.designsystem.component.UthSectionHeader
import com.giathinh.uthconnect.core.designsystem.theme.UthDimens
import com.giathinh.uthconnect.core.designsystem.theme.UthPreview
import com.giathinh.uthconnect.core.designsystem.theme.UthTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    uiState: SettingsUiState = SettingsUiState(),
    onToggleTuitionReminder: (Boolean) -> Unit = {},
    onToggleTrainingPointNotif: (Boolean) -> Unit = {},
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
                            contentDescription = "Quay lại",
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
                HorizontalDivider(color = DividerDefaults.color.copy(alpha = 0.5f))
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
                HorizontalDivider(color = DividerDefaults.color.copy(alpha = 0.5f))
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
