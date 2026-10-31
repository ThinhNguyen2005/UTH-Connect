package com.giathinh.uthconnect.presentation.utilities

import androidx.annotation.StringRes
import androidx.compose.foundation.background
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.giathinh.uthconnect.R
import com.giathinh.uthconnect.core.designsystem.component.BadgeTone
import com.giathinh.uthconnect.core.designsystem.component.UthCard
import com.giathinh.uthconnect.core.designsystem.component.UthEmptyState
import com.giathinh.uthconnect.core.designsystem.component.UthSectionHeader
import com.giathinh.uthconnect.core.designsystem.component.UthStatusBadge
import com.giathinh.uthconnect.core.designsystem.theme.UthDimens
import com.giathinh.uthconnect.core.designsystem.theme.UthPreview
import com.giathinh.uthconnect.core.designsystem.theme.UthTheme
import com.giathinh.uthconnect.domain.model.DeliveryPhase
import com.giathinh.uthconnect.domain.model.UtilityModule
import com.giathinh.uthconnect.domain.model.UtilityModuleId

/**
 * Screen **stateless**: chỉ render [UtilitiesUiState] và phát [UtilitiesAction]
 * hoặc kích hoạt các callback điều hướng con.
 */
@Composable
fun UtilitiesScreen(
    uiState: UtilitiesUiState,
    onAction: (UtilitiesAction) -> Unit,
    onNavigateToTuition: () -> Unit = {},
    onNavigateToTrainingPoint: () -> Unit = {},
    onNavigateToCampuses: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToLostFound: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = UthDimens.Spacing.Xl),
    ) {
        Spacer(Modifier.height(UthDimens.Spacing.Xl))
        Text(
            text = stringResource(R.string.utilities_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(UthDimens.Spacing.Xs))
        Text(
            text = stringResource(R.string.utilities_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = UthTheme.extendedColors.textSecondary,
        )
        Spacer(Modifier.height(UthDimens.Spacing.Lg))

        // Lưới Grid 4 dịch vụ tiện ích sinh viên chính
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Md),
        ) {
            UtilityGridCard(
                title = stringResource(R.string.utilities_grid_tuition),
                subtitle = stringResource(R.string.utilities_grid_tuition_desc),
                icon = Icons.Filled.DateRange,
                onClick = onNavigateToTuition,
                modifier = Modifier.weight(1f),
            )
            UtilityGridCard(
                title = stringResource(R.string.utilities_grid_training_point),
                subtitle = stringResource(R.string.utilities_grid_training_point_desc),
                icon = Icons.Filled.Star,
                onClick = onNavigateToTrainingPoint,
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(Modifier.height(UthDimens.Spacing.Md))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Md),
        ) {
            UtilityGridCard(
                title = stringResource(R.string.utilities_grid_campuses),
                subtitle = stringResource(R.string.utilities_grid_campuses_desc),
                icon = Icons.Filled.Place,
                onClick = onNavigateToCampuses,
                modifier = Modifier.weight(1f),
            )
            UtilityGridCard(
                title = stringResource(R.string.utilities_grid_settings),
                subtitle = stringResource(R.string.utilities_grid_settings_desc),
                icon = Icons.Filled.Settings,
                onClick = onNavigateToSettings,
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(Modifier.height(UthDimens.Spacing.Section))

        // Khu vực các dịch vụ lộ trình tiếp theo
        UthSectionHeader(title = stringResource(R.string.utilities_section_upcoming))

        when {
            uiState.isLoading -> UtilitiesLoading()
            uiState.failure != null -> UtilitiesError(
                onRetry = { onAction(UtilitiesAction.Retry) },
            )
            uiState.isEmpty -> UthEmptyState(
                title = stringResource(R.string.utilities_empty_title),
                body = stringResource(R.string.utilities_empty_body),
            )
            else -> uiState.modules.forEach { module ->
                UthModuleRow(
                    module = module,
                    onClick = {
                        if (module.id == UtilityModuleId.LOST_FOUND) {
                            onNavigateToLostFound()
                        } else {
                            onAction(UtilitiesAction.OpenModule(module.id))
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = UthDimens.Spacing.Md),
                )
            }
        }

        Spacer(Modifier.height(UthDimens.Spacing.Xl))
    }
}

/**
 * Card dịch vụ dạng ô lưới Grid.
 */
@Composable
private fun UtilityGridCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    UthCard(
        modifier = modifier,
        onClick = onClick,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.small,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(24.dp),
            )
        }

        Spacer(Modifier.height(UthDimens.Spacing.Md))

        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )

        Spacer(Modifier.height(UthDimens.Spacing.Xs))

        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = UthTheme.extendedColors.textSecondary,
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

@Composable
private fun UthModuleRow(
    module: UtilityModule,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val (tone, labelRes) = module.phase.badgeSpec()
    UthCard(
        modifier = modifier,
        onClick = onClick,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(module.id.titleRes()),
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(UthDimens.Spacing.Xs))
                Text(
                    text = stringResource(module.id.descRes()),
                    style = MaterialTheme.typography.bodySmall,
                    color = UthTheme.extendedColors.textSecondary,
                )
            }
            Spacer(Modifier.width(UthDimens.Spacing.Sm))
            UthStatusBadge(text = stringResource(labelRes), tone = tone)
        }
    }
}

private fun DeliveryPhase.badgeSpec(): Pair<BadgeTone, Int> = when (this) {
    DeliveryPhase.MVP -> BadgeTone.Accent to R.string.phase_mvp
    DeliveryPhase.PHASE_2 -> BadgeTone.Info to R.string.phase_2
    DeliveryPhase.PHASE_3 -> BadgeTone.Info to R.string.phase_3
    DeliveryPhase.BACKLOG -> BadgeTone.Neutral to R.string.phase_backlog
}

@StringRes
private fun UtilityModuleId.titleRes(): Int = when (this) {
    UtilityModuleId.EVENTS -> R.string.module_events_title
    UtilityModuleId.TIMETABLE -> R.string.module_timetable_title
    UtilityModuleId.LOST_FOUND -> R.string.module_lost_found_title
    UtilityModuleId.BOOKS -> R.string.module_books_title
    UtilityModuleId.HOUSING -> R.string.module_housing_title
    UtilityModuleId.RIDE_SHARE -> R.string.module_rideshare_title
}

@StringRes
private fun UtilityModuleId.descRes(): Int = when (this) {
    UtilityModuleId.EVENTS -> R.string.module_events_desc
    UtilityModuleId.TIMETABLE -> R.string.module_timetable_desc
    UtilityModuleId.LOST_FOUND -> R.string.module_lost_found_desc
    UtilityModuleId.BOOKS -> R.string.module_books_desc
    UtilityModuleId.HOUSING -> R.string.module_housing_desc
    UtilityModuleId.RIDE_SHARE -> R.string.module_rideshare_desc
}

private val PreviewModules = listOf(
    UtilityModule(UtilityModuleId.EVENTS, DeliveryPhase.MVP, enabled = true),
    UtilityModule(UtilityModuleId.TIMETABLE, DeliveryPhase.PHASE_2, enabled = false),
    UtilityModule(UtilityModuleId.LOST_FOUND, DeliveryPhase.PHASE_3, enabled = false),
    UtilityModule(UtilityModuleId.BOOKS, DeliveryPhase.BACKLOG, enabled = false),
)

@Composable
private fun UtilitiesStatePreview(state: UtilitiesUiState, darkTheme: Boolean = false) =
    UthPreview(darkTheme = darkTheme) {
        UtilitiesScreen(
            uiState = state,
            onAction = {},
        )
    }

@Preview(name = "content light", showBackground = true)
@Composable
private fun UtilitiesContentPreview() =
    UtilitiesStatePreview(UtilitiesUiState(isLoading = false, modules = PreviewModules))

@Preview(name = "content dark", showBackground = true)
@Composable
private fun UtilitiesContentDarkPreview() =
    UtilitiesStatePreview(UtilitiesUiState(isLoading = false, modules = PreviewModules), darkTheme = true)

@Preview(name = "loading light", showBackground = true)
@Composable
private fun UtilitiesLoadingPreview() = UtilitiesStatePreview(UtilitiesUiState(isLoading = true))

@Preview(name = "error light", showBackground = true)
@Composable
private fun UtilitiesErrorPreview() = UtilitiesStatePreview(
    UtilitiesUiState(isLoading = false, failure = UtilitiesFailure.LoadFailed),
)
