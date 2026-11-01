package com.giathinh.uthconnect.presentation.utilities.tuition

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
fun TuitionScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    uiState: TuitionUiState = TuitionUiState(),
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.tuition_title),
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

            // Thẻ tóm tắt trạng thái học phí
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
                        text = if (uiState.isPaid) {
                            stringResource(R.string.tuition_status_paid)
                        } else {
                            stringResource(R.string.tuition_status_unpaid)
                        },
                        tone = if (uiState.isPaid) BadgeTone.Success else BadgeTone.Warning,
                    )
                }

                Spacer(Modifier.height(UthDimens.Spacing.Lg))

                Text(
                    text = stringResource(R.string.tuition_total_amount),
                    style = MaterialTheme.typography.bodyMedium,
                    color = UthTheme.extendedColors.textSecondary,
                )
                Spacer(Modifier.height(UthDimens.Spacing.Xs))
                Text(
                    text = uiState.totalAmount,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )

                Spacer(Modifier.height(UthDimens.Spacing.Md))
                HorizontalDivider(color = DividerDefaults.color.copy(alpha = 0.5f))
                Spacer(Modifier.height(UthDimens.Spacing.Md))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.tuition_paid_amount),
                            style = MaterialTheme.typography.bodySmall,
                            color = UthTheme.extendedColors.textSecondary,
                        )
                        Spacer(Modifier.height(UthDimens.Spacing.Xs))
                        Text(
                            text = uiState.paidAmount,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = stringResource(R.string.tuition_remaining_amount),
                            style = MaterialTheme.typography.bodySmall,
                            color = UthTheme.extendedColors.textSecondary,
                        )
                        Spacer(Modifier.height(UthDimens.Spacing.Xs))
                        Text(
                            text = uiState.remainingAmount,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (uiState.remainingAmount == "0 VNĐ") {
                                UthTheme.extendedColors.onSuccessContainer
                            } else {
                                UthTheme.extendedColors.onDangerContainer
                            },
                        )
                    }
                }

                Spacer(Modifier.height(UthDimens.Spacing.Sm))
                Text(
                    text = stringResource(R.string.tuition_credits_count, uiState.creditsCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = UthTheme.extendedColors.textSecondary,
                )
                Text(
                    text = stringResource(R.string.tuition_due_date, uiState.dueDate),
                    style = MaterialTheme.typography.bodySmall,
                    color = UthTheme.extendedColors.textSecondary,
                )
            }

            // Danh sách môn học đã đăng ký
            UthSectionHeader(title = stringResource(R.string.tuition_courses_header))

            uiState.courses.forEach { course ->
                UthCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = UthDimens.Spacing.Sm),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = course.name,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Spacer(Modifier.height(UthDimens.Spacing.Xs))
                            Text(
                                text = "${course.code} • ${course.credits} tín chỉ",
                                style = MaterialTheme.typography.bodySmall,
                                color = UthTheme.extendedColors.textSecondary,
                            )
                        }
                        Spacer(Modifier.width(UthDimens.Spacing.Md))
                        Text(
                            text = course.fee,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }

            // Hướng dẫn chuyển khoản
            UthSectionHeader(title = stringResource(R.string.tuition_payment_guide_header))

            UthCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = UthDimens.Spacing.Xl),
            ) {
                Text(
                    text = stringResource(R.string.tuition_bank_account_name),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                )
                Spacer(Modifier.height(UthDimens.Spacing.Xs))
                Text(
                    text = stringResource(R.string.tuition_bank_account_number),
                    style = MaterialTheme.typography.bodyMedium,
                    color = UthTheme.extendedColors.textSecondary,
                )
                Spacer(Modifier.height(UthDimens.Spacing.Xs))
                Text(
                    text = stringResource(R.string.tuition_payment_syntax),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Preview(name = "TuitionScreen Light", showBackground = true)
@Composable
private fun TuitionScreenPreview() {
    UthPreview {
        TuitionScreen(onBack = {})
    }
}

@Preview(name = "TuitionScreen Dark", showBackground = true)
@Composable
private fun TuitionScreenDarkPreview() {
    UthPreview(darkTheme = true) {
        TuitionScreen(onBack = {})
    }
}
