package com.giathinh.uthconnect.presentation.utilities

import androidx.annotation.StringRes
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
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.giathinh.uthconnect.R
import com.giathinh.uthconnect.core.designsystem.component.BadgeTone
import com.giathinh.uthconnect.core.designsystem.component.UthCard
import com.giathinh.uthconnect.core.designsystem.component.UthEmptyState
import com.giathinh.uthconnect.core.designsystem.component.UthStatusBadge
import com.giathinh.uthconnect.core.designsystem.theme.UthDimens
import com.giathinh.uthconnect.core.designsystem.theme.UthPreview
import com.giathinh.uthconnect.core.designsystem.theme.UthTheme
import com.giathinh.uthconnect.domain.model.DeliveryPhase
import com.giathinh.uthconnect.domain.model.UtilityModule
import com.giathinh.uthconnect.domain.model.UtilityModuleId

/**
 * Screen **stateless**: chỉ render [UtilitiesUiState] và phát [UtilitiesAction].
 * Không repository, không `hiltViewModel()` trong file này — đó là ràng buộc tầng
 * `ui` trong `README.md` mục 5.2 và là lý do mọi trạng thái dưới đây preview được.
 */
@Composable
fun UtilitiesScreen(
    uiState: UtilitiesUiState,
    onAction: (UtilitiesAction) -> Unit,
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
        )
        Spacer(Modifier.height(UthDimens.Spacing.Xs))
        Text(
            text = stringResource(R.string.utilities_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = UthTheme.extendedColors.textSecondary,
        )
        Spacer(Modifier.height(UthDimens.Spacing.Section))

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
                    onClick = { onAction(UtilitiesAction.OpenModule(module.id)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = UthDimens.Spacing.Md),
                )
            }
        }
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
    // enum → chuỗi: phần tử map đặt ở UI, vì ViewModel không được cầm @StringRes.
    UthEmptyState(
        title = stringResource(R.string.utilities_error_title),
        body = stringResource(R.string.utilities_error_body),
        icon = Icons.Filled.Warning,
        actionLabel = stringResource(R.string.common_retry),
        onAction = onRetry,
    )
}

/**
 * Card module. `onClick` chỉ phát action — xử lý gì (mở graph con, hiện "sắp ra
 * mắt") là quyết định của ViewModel và branch phụ trách module đó.
 */
@Composable
private fun UthModuleRow(
    module: UtilityModule,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val (tone, labelRes) = module.phase.badgeSpec()
    UthCard(
        modifier = modifier,
        onClick = if (module.enabled) onClick else null,
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
                if (!module.enabled) {
                    Spacer(Modifier.height(UthDimens.Spacing.Xs))
                    Text(
                        text = stringResource(R.string.utilities_not_available),
                        style = MaterialTheme.typography.labelSmall,
                        color = UthTheme.extendedColors.textSecondary,
                    )
                }
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

/** Dữ liệu giả cho preview — không phải nguồn thật, chỉ để dựng 4 trạng thái. */
private val PreviewModules = listOf(
    UtilityModule(UtilityModuleId.EVENTS, DeliveryPhase.MVP, enabled = true),
    UtilityModule(UtilityModuleId.TIMETABLE, DeliveryPhase.PHASE_2, enabled = false),
    UtilityModule(UtilityModuleId.LOST_FOUND, DeliveryPhase.PHASE_3, enabled = false),
    UtilityModule(UtilityModuleId.BOOKS, DeliveryPhase.BACKLOG, enabled = false),
)

@Composable
private fun UtilitiesStatePreview(state: UtilitiesUiState, darkTheme: Boolean = false) =
    UthPreview(darkTheme = darkTheme) { UtilitiesScreen(uiState = state, onAction = {}) }

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

@Preview(name = "loading dark", showBackground = true)
@Composable
private fun UtilitiesLoadingDarkPreview() =
    UtilitiesStatePreview(UtilitiesUiState(isLoading = true), darkTheme = true)

@Preview(name = "error light", showBackground = true)
@Composable
private fun UtilitiesErrorPreview() = UtilitiesStatePreview(
    UtilitiesUiState(isLoading = false, failure = UtilitiesFailure.LoadFailed),
)

@Preview(name = "error dark", showBackground = true)
@Composable
private fun UtilitiesErrorDarkPreview() = UtilitiesStatePreview(
    UtilitiesUiState(isLoading = false, failure = UtilitiesFailure.LoadFailed),
    darkTheme = true,
)

@Preview(name = "empty light", showBackground = true)
@Composable
private fun UtilitiesEmptyPreview() = UtilitiesStatePreview(UtilitiesUiState(isLoading = false))

@Preview(name = "empty dark", showBackground = true)
@Composable
private fun UtilitiesEmptyDarkPreview() =
    UtilitiesStatePreview(UtilitiesUiState(isLoading = false), darkTheme = true)
