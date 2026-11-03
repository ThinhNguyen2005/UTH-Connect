package com.giathinh.uthconnect.presentation.utilities

import android.widget.Toast
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.giathinh.uthconnect.R
import com.giathinh.uthconnect.core.designsystem.component.UthCard
import com.giathinh.uthconnect.core.designsystem.component.UthEmptyState
import com.giathinh.uthconnect.core.designsystem.component.UthSectionHeader
import com.giathinh.uthconnect.core.designsystem.theme.UthDimens
import com.giathinh.uthconnect.core.designsystem.theme.UthPreview
import com.giathinh.uthconnect.core.designsystem.theme.UthTheme
import com.giathinh.uthconnect.domain.model.UtilityModuleId

/**
 * Screen **stateless**: Hiển thị Trung tâm Tiện ích theo phong cách Lưới dịch vụ MoMo.
 */
@Composable
fun UtilitiesScreen(
    uiState: UtilitiesUiState,
    onAction: (UtilitiesAction) -> Unit,
    onNavigateToTuition: () -> Unit = {},
    onNavigateToTrainingPoint: () -> Unit = {},
    onNavigateToCampuses: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = UthDimens.Spacing.Xl),
    ) {
        Spacer(Modifier.height(UthDimens.Spacing.Md))

        // Top Header với nút Quay lại và Tiêu đề
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.common_back),
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
            Spacer(Modifier.width(UthDimens.Spacing.Xs))
            Column {
                Text(
                    text = stringResource(R.string.utilities_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = stringResource(R.string.utilities_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = UthTheme.extendedColors.textSecondary,
                )
            }
        }

        Spacer(Modifier.height(UthDimens.Spacing.Lg))

        when {
            uiState.isLoading -> UtilitiesLoading()
            uiState.failure != null -> UtilitiesError(
                onRetry = { onAction(UtilitiesAction.Retry) },
            )
            else -> {
                // Nhóm 1: Tiện ích Học tập & Quản lý
                UthSectionHeader(title = "Dịch vụ Học tập & Quản lý")
                UthCard(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = UthDimens.Spacing.Sm),
                        horizontalArrangement = Arrangement.SpaceAround,
                    ) {
                        MomoGridItem(
                            title = stringResource(R.string.utilities_grid_tuition),
                            icon = Icons.Filled.DateRange,
                            containerColor = Color(0xFFE8F0FE),
                            iconTint = Color(0xFF1967D2),
                            onClick = onNavigateToTuition,
                            modifier = Modifier.weight(1f),
                        )
                        MomoGridItem(
                            title = stringResource(R.string.utilities_grid_training_point),
                            icon = Icons.Filled.Star,
                            containerColor = Color(0xFFFEF7E0),
                            iconTint = Color(0xFFF29900),
                            onClick = onNavigateToTrainingPoint,
                            modifier = Modifier.weight(1f),
                        )
                        MomoGridItem(
                            title = stringResource(R.string.utilities_grid_campuses),
                            icon = Icons.Filled.Place,
                            containerColor = Color(0xFFE6F4EA),
                            iconTint = Color(0xFF137333),
                            onClick = onNavigateToCampuses,
                            modifier = Modifier.weight(1f),
                        )
                        MomoGridItem(
                            title = stringResource(R.string.utilities_grid_settings),
                            icon = Icons.Filled.Settings,
                            containerColor = Color(0xFFF1F3F4),
                            iconTint = Color(0xFF5F6368),
                            onClick = onNavigateToSettings,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                Spacer(Modifier.height(UthDimens.Spacing.Lg))

                // Nhóm 2: Đời sống & Tiện ích Sinh viên (MoMo Grid)
                UthSectionHeader(title = "Đời sống & Hỗ trợ Sinh viên")
                UthCard(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = UthDimens.Spacing.Sm),
                        horizontalArrangement = Arrangement.SpaceAround,
                    ) {
                        MomoGridItem(
                            title = stringResource(R.string.module_lost_found_title),
                            icon = Icons.Filled.Search,
                            containerColor = Color(0xFFF3E8FD),
                            iconTint = Color(0xFF9334E6),
                            onClick = { onAction(UtilitiesAction.OpenModule(UtilityModuleId.LOST_FOUND)) },
                            modifier = Modifier.weight(1f),
                        )
                        MomoGridItem(
                            title = stringResource(R.string.module_books_title),
                            icon = Icons.Filled.ShoppingCart,
                            containerColor = Color(0xFFFCE8E6),
                            iconTint = Color(0xFFC5221F),
                            onClick = {
                                Toast.makeText(context, "Chợ giáo trình đang được hoàn thiện!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f),
                        )
                        MomoGridItem(
                            title = stringResource(R.string.module_housing_title),
                            icon = Icons.Filled.Home,
                            containerColor = Color(0xFFE8F4F8),
                            iconTint = Color(0xFF1A73E8),
                            onClick = {
                                Toast.makeText(context, "Tìm phòng ghép trọ đang được hoàn thiện!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f),
                        )
                        MomoGridItem(
                            title = stringResource(R.string.module_rideshare_title),
                            icon = Icons.Filled.Share,
                            containerColor = Color(0xFFE6F7F0),
                            iconTint = Color(0xFF0D904F),
                            onClick = {
                                Toast.makeText(context, "Dịch vụ ghép xe đang được hoàn thiện!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(UthDimens.Spacing.Xxl))
    }
}

/**
 * Item tiện ích dạng nút ứng dụng bo góc kiểu MoMo.
 */
@Composable
private fun MomoGridItem(
    title: String,
    icon: ImageVector,
    containerColor: Color,
    iconTint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(UthDimens.Radius.MdShape)
            .clickable(onClick = onClick)
            .padding(vertical = UthDimens.Spacing.Sm, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .background(containerColor, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = iconTint,
                modifier = Modifier.size(26.dp),
            )
        }
        Spacer(Modifier.height(UthDimens.Spacing.Sm))
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 14.sp,
        )
    }
}

@Composable
private fun UtilitiesLoading() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = UthDimens.Spacing.Xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(UthDimens.Size.Progress),
            color = MaterialTheme.colorScheme.primary,
            strokeWidth = UthDimens.Size.ProgressStroke,
        )
        Spacer(Modifier.height(UthDimens.Spacing.Md))
        Text(
            text = stringResource(R.string.utilities_loading),
            style = MaterialTheme.typography.bodyMedium,
            color = UthTheme.extendedColors.textSecondary,
        )
    }
}

@Composable
private fun UtilitiesError(onRetry: () -> Unit) {
    UthEmptyState(
        title = stringResource(R.string.utilities_error_title),
        body = stringResource(R.string.utilities_error_body),
        icon = Icons.Filled.Warning,
        actionLabel = stringResource(R.string.common_retry),
        onAction = onRetry,
    )
}

@Preview(name = "Momo Grid Utilities", showBackground = true)
@Composable
private fun UtilitiesContentPreview() =
    UthPreview {
        UtilitiesScreen(
            uiState = UtilitiesUiState(isLoading = false),
            onAction = {},
        )
    }
