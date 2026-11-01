package com.giathinh.uthconnect.presentation.utilities.trainingpoint

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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.giathinh.uthconnect.R
import com.giathinh.uthconnect.core.designsystem.component.BadgeTone
import com.giathinh.uthconnect.core.designsystem.component.UthCard
import com.giathinh.uthconnect.core.designsystem.component.UthSectionHeader
import com.giathinh.uthconnect.core.designsystem.component.UthStatusBadge
import com.giathinh.uthconnect.core.designsystem.theme.UthDimens
import com.giathinh.uthconnect.core.designsystem.theme.UthPreview
import com.giathinh.uthconnect.core.designsystem.theme.UthTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainingPointScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    uiState: TrainingPointUiState = TrainingPointUiState(),
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.training_point_title),
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

            // Card Điểm tổng kết nổi bật
            UthCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = uiState.semester,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    UthStatusBadge(
                        text = "XẾP LOẠI: ${uiState.rank}",
                        tone = BadgeTone.Success,
                    )
                }

                Spacer(Modifier.height(UthDimens.Spacing.Lg))

                Text(
                    text = stringResource(R.string.training_point_total_label),
                    style = MaterialTheme.typography.bodyMedium,
                    color = UthTheme.extendedColors.textSecondary,
                )
                Spacer(Modifier.height(UthDimens.Spacing.Xs))
                Row(
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Text(
                        text = "${uiState.totalScore}",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = " / ${uiState.maxScore} điểm",
                        style = MaterialTheme.typography.titleMedium,
                        color = UthTheme.extendedColors.textSecondary,
                        modifier = Modifier.padding(bottom = 8.dp, start = 4.dp),
                    )
                }

                Spacer(Modifier.height(UthDimens.Spacing.Md))
                LinearProgressIndicator(
                    progress = { uiState.totalScore.toFloat() / uiState.maxScore.toFloat() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            }

            // Danh sách từng tiêu chí chi tiết
            UthSectionHeader(title = stringResource(R.string.training_point_criteria_header))

            uiState.criteriaList.forEach { criteria ->
                UthCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = UthDimens.Spacing.Md),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = criteria.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = "${criteria.score}/${criteria.maxScore} đ",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }

                    Spacer(Modifier.height(UthDimens.Spacing.Xs))
                    Text(
                        text = criteria.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = UthTheme.extendedColors.textSecondary,
                    )

                    Spacer(Modifier.height(UthDimens.Spacing.Sm))
                    LinearProgressIndicator(
                        progress = { criteria.score.toFloat() / criteria.maxScore.toFloat() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp),
                        color = MaterialTheme.colorScheme.secondary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(UthDimens.Spacing.Lg))
        }
    }
}

@Preview(name = "TrainingPointScreen Light", showBackground = true)
@Composable
private fun TrainingPointScreenPreview() {
    UthPreview {
        TrainingPointScreen(onBack = {})
    }
}

@Preview(name = "TrainingPointScreen Dark", showBackground = true)
@Composable
private fun TrainingPointScreenDarkPreview() {
    UthPreview(darkTheme = true) {
        TrainingPointScreen(onBack = {})
    }
}
