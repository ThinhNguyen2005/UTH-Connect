package com.giathinh.uthconnect.presentation.utilities

import androidx.annotation.DrawableRes
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import com.giathinh.uthconnect.R
import com.giathinh.uthconnect.core.designsystem.component.UthCard
import com.giathinh.uthconnect.core.designsystem.component.UthContentSkeleton
import com.giathinh.uthconnect.core.designsystem.component.UthSectionHeader
import com.giathinh.uthconnect.core.designsystem.theme.UthDimens
import com.giathinh.uthconnect.core.designsystem.theme.UthPreview
import com.giathinh.uthconnect.core.designsystem.theme.UthTheme
import com.giathinh.uthconnect.domain.model.DeliveryPhase
import com.giathinh.uthconnect.domain.model.UtilityModule
import com.giathinh.uthconnect.domain.model.UtilityModuleId

/**
 * Screen **stateless**: chỉ render [UtilitiesUiState] và phát [UtilitiesAction]
 * hoặc kích hoạt các callback điều hướng con.
 *
 * Bố cục hai khu vực theo ảnh tham chiếu của các super-app Việt:
 * - **"Tiện ích của tôi"**: tile lớn 2 cột (khung icon `Size.UtilityFeatureIconContainer`,
 *   glyph duotone `Size.UtilityFeatureIcon`, tiêu đề và mô tả) cho mọi dịch vụ mở được
 *   ngay — 4 tiện ích tra cứu của sinh viên cộng với module đã bật
 *   ([UtilityModule.enabled]) từ repository. Sau khi bottom nav rút còn 4 tab,
 *   đây là điểm vào duy nhất của Sự kiện và Đồ thất lạc.
 * - **"Dịch vụ đang phát triển"**: lưới nén 3 cột (icon + nhãn + lộ trình), không bấm được.
 *
 * Mật độ 3 cột chỉ dành cho mục **chưa mở được**: với lề `Spacing.Xl` thì trên màn hẹp nhất
 * mỗi ô còn hơn 80dp bề ngang, đủ cho khung icon dịch vụ nhưng không đủ cho badge màu, nên
 * trạng thái diễn đạt bằng chữ (`phase_*`)
 * thay vì chỉ bằng màu — đúng checklist `docs/UI-UX.md`.
 */
@Composable
fun UtilitiesScreen(
    uiState: UtilitiesUiState,
    onAction: (UtilitiesAction) -> Unit,
    onNavigateToTuition: () -> Unit = {},
    onNavigateToTrainingPoint: () -> Unit = {},
    onNavigateToCampuses: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val openedModules = uiState.modules.filter { it.enabled }

    /** Một lần bấm ô lưới luôn đi qua đây, nên tile không cần giữ lambda. */
    fun onTileTarget(target: UtilityTileTarget) {
        when (target) {
            UtilityTileTarget.Tuition -> onNavigateToTuition()
            UtilityTileTarget.TrainingPoint -> onNavigateToTrainingPoint()
            UtilityTileTarget.Campuses -> onNavigateToCampuses()
            UtilityTileTarget.Settings -> onNavigateToSettings()
            is UtilityTileTarget.Module -> onAction(UtilitiesAction.OpenModule(target.moduleId))
        }
    }

    // Một hàng đợi duy nhất cho "thứ sinh viên mở được ngay": 4 tiện ích tra cứu
    // và module đã bật, để hai khu vực không cùng tranh sự chú ý.
    val myUtilities = listOf(
//        UtilityTile(
//            icon = R.drawable.ic_utilities_tuition,
//            title = stringResource(R.string.utilities_grid_tuition),
//            subtitle = stringResource(R.string.utilities_grid_tuition_desc),
//            target = UtilityTileTarget.Tuition,
//        ),
//        UtilityTile(
//            icon = R.drawable.ic_utilities_training_point,
//            title = stringResource(R.string.utilities_grid_training_point),
//            subtitle = stringResource(R.string.utilities_grid_training_point_desc),
//            target = UtilityTileTarget.TrainingPoint,
//        ),
        UtilityTile(
            icon = R.drawable.ic_utilities_campuses,
            title = stringResource(R.string.utilities_grid_campuses),
            subtitle = stringResource(R.string.utilities_grid_campuses_desc),
            target = UtilityTileTarget.Campuses,
        ),
        UtilityTile(
            icon = R.drawable.ic_utilities_settings,
            title = stringResource(R.string.utilities_grid_settings),
            subtitle = stringResource(R.string.utilities_grid_settings_desc),
            target = UtilityTileTarget.Settings,
        ),
    ) + openedModules.map { module ->
        UtilityTile(
            icon = module.id.iconRes(),
            title = stringResource(module.id.titleRes()),
            subtitle = stringResource(module.id.descRes()),
            target = UtilityTileTarget.Module(module.id),
        )
    }

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
        UthSectionHeader(title = stringResource(R.string.utilities_section_mine))
        Column(
            verticalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Sm),
            modifier = Modifier.fillMaxWidth(),
        ) {
            myUtilities.forEach { tile ->
                UtilityListItem(
                    tile = tile,
                    onClick = { onTileTarget(tile.target) },
                )
            }
        }

        if (uiState.isLoading && uiState.modules.isEmpty()) UthContentSkeleton()

        Spacer(Modifier.height(UthDimens.Spacing.Xl))
    }
}

/**
 * Mục tiện ích hiển thị dạng hàng danh sách (List Item): icon khối vuông bo tròn bên trái,
 * tiêu đề và mô tả ở giữa, mũi tên điều hướng bên phải.
 */
@Composable
private fun UtilityListItem(
    tile: UtilityTile,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    UthCard(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = UthDimens.Size.MinTouchTarget),
        onClick = onClick,
        contentPadding = false,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = UthDimens.Spacing.Lg, vertical = UthDimens.Spacing.Md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            UtilityIconFrame(
                iconRes = tile.icon,
                container = UthDimens.Size.UtilityIconContainer,
                icon = UthDimens.Size.Icon,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            )

            Spacer(Modifier.width(UthDimens.Spacing.Md))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tile.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (tile.subtitle.isNotBlank()) {
                    Spacer(Modifier.height(UthDimens.Spacing.Xxs))
                    Text(
                        text = tile.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = UthTheme.extendedColors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Spacer(Modifier.width(UthDimens.Spacing.Sm))

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(UthDimens.Size.IconCompact),
            )
        }
    }
}

/**
 * Khung vuông bo góc chứa glyph: kích thước, màu nền và màu icon đều lấy từ design
 * token.
 */
@Composable
private fun UtilityIconFrame(
    @DrawableRes iconRes: Int,
    container: Dp,
    icon: Dp,
    containerColor: Color,
    contentColor: Color,
) {
    Box(
        modifier = Modifier
            .size(container)
            .background(
                color = containerColor,
                shape = MaterialTheme.shapes.small,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(icon),
        )
    }
}

/** Một ô dịch vụ, nhãn đã giải quyết thành chuỗi để [UtilityTileGrid] không cần biết resource id. */
@Immutable
private data class UtilityTile(
    @param:DrawableRes val icon: Int,
    val title: String,
    val subtitle: String,
    val target: UtilityTileTarget,
)

/**
 * Điểm đến của một ô lưới. Để [UtilityTile] thuần dữ liệu (không giữ lambda) — lambda
 * ở lại phía screen, xem [UtilitiesScreen.onTileTarget].
 */
@Immutable
private sealed interface UtilityTileTarget {
    data object Tuition : UtilityTileTarget
    data object TrainingPoint : UtilityTileTarget
    data object Campuses : UtilityTileTarget
    data object Settings : UtilityTileTarget
    data class Module(val moduleId: UtilityModuleId) : UtilityTileTarget
}

/**
 * Ánh xạ module → glyph duotone trong `res/drawable` (nguồn: Material Icons
 * two-tone của Google, Apache-2.0; xem chú thích đầu mỗi file vector).
 */
@DrawableRes
private fun UtilityModuleId.iconRes(): Int = when (this) {
    UtilityModuleId.EVENTS -> R.drawable.ic_utilities_events
    UtilityModuleId.TIMETABLE -> R.drawable.ic_utilities_timetable
    UtilityModuleId.LOST_FOUND -> R.drawable.ic_utilities_lost_found
    UtilityModuleId.BOOKS -> R.drawable.ic_utilities_books
    UtilityModuleId.HOUSING -> R.drawable.ic_utilities_housing
    UtilityModuleId.INCIDENTS -> R.drawable.ic_utilities_incidents
    UtilityModuleId.RIDE_SHARE -> R.drawable.ic_utilities_rideshare
}

@StringRes
private fun UtilityModuleId.titleRes(): Int = when (this) {
    UtilityModuleId.EVENTS -> R.string.module_events_title
    UtilityModuleId.TIMETABLE -> R.string.module_timetable_title
    UtilityModuleId.LOST_FOUND -> R.string.module_lost_found_title
    UtilityModuleId.BOOKS -> R.string.module_books_title
    UtilityModuleId.HOUSING -> R.string.module_housing_title
    UtilityModuleId.INCIDENTS -> R.string.module_incidents_title
    UtilityModuleId.RIDE_SHARE -> R.string.module_rideshare_title
}

@StringRes
private fun UtilityModuleId.descRes(): Int = when (this) {
    UtilityModuleId.EVENTS -> R.string.module_events_desc
    UtilityModuleId.TIMETABLE -> R.string.module_timetable_desc
    UtilityModuleId.LOST_FOUND -> R.string.module_lost_found_desc
    UtilityModuleId.BOOKS -> R.string.module_books_desc
    UtilityModuleId.HOUSING -> R.string.module_housing_desc
    UtilityModuleId.INCIDENTS -> R.string.module_incidents_desc
    UtilityModuleId.RIDE_SHARE -> R.string.module_rideshare_desc
}

private val PreviewModules = listOf(
    UtilityModule(UtilityModuleId.EVENTS, DeliveryPhase.MVP, enabled = true),
    UtilityModule(UtilityModuleId.TIMETABLE, DeliveryPhase.PHASE_2, enabled = false),
    UtilityModule(UtilityModuleId.LOST_FOUND, DeliveryPhase.PHASE_3, enabled = true),
    UtilityModule(UtilityModuleId.BOOKS, DeliveryPhase.BACKLOG, enabled = false),
    UtilityModule(UtilityModuleId.HOUSING, DeliveryPhase.BACKLOG, enabled = true),
    UtilityModule(UtilityModuleId.RIDE_SHARE, DeliveryPhase.BACKLOG, enabled = false),
)

@Composable
private fun UtilitiesStatePreview(state: UtilitiesUiState, darkTheme: Boolean = false) = UthPreview(darkTheme = darkTheme) {
    UtilitiesScreen(
        uiState = state,
        onAction = {},
    )
}

@Preview(name = "content light", showBackground = true)
@Composable
private fun UtilitiesContentPreview() = UtilitiesStatePreview(UtilitiesUiState(isLoading = false, modules = PreviewModules))

@Preview(name = "content dark", showBackground = true)
@Composable
private fun UtilitiesContentDarkPreview() = UtilitiesStatePreview(UtilitiesUiState(isLoading = false, modules = PreviewModules), darkTheme = true)

// Font scale 1.5: lưới 2 cột phải wrapping chứ không cắt nhãn (docs/UI-UX.md mục phóng to chữ);
// khu 3 cột tự hạ về 2 cột theo Layout.ServiceGridColumnsLargeText.
@Preview(name = "content large text", showBackground = true, fontScale = 1.5f)
@Composable
private fun UtilitiesContentLargeTextPreview() = UtilitiesStatePreview(UtilitiesUiState(isLoading = false, modules = PreviewModules))

// Màn hẹp nhất còn hỗ trợ: bề ngang chữ của ô 3 cột xuống ~66dp, cả hai khu phải còn
// đọc được (nhãn gãy dòng có dấu "…", hai card cùng hàng bằng đáy).
@Preview(name = "content narrow", showBackground = true, widthDp = 320, heightDp = 640)
@Composable
private fun UtilitiesContentNarrowPreview() = UtilitiesStatePreview(UtilitiesUiState(isLoading = false, modules = PreviewModules))

@Preview(name = "content narrow large text", showBackground = true, widthDp = 320, heightDp = 640, fontScale = 1.5f)
@Composable
private fun UtilitiesContentNarrowLargeTextPreview() = UtilitiesStatePreview(UtilitiesUiState(isLoading = false, modules = PreviewModules))

@Preview(name = "loading light", showBackground = true)
@Composable
private fun UtilitiesLoadingPreview() = UtilitiesStatePreview(UtilitiesUiState(isLoading = true))

@Preview(name = "loading dark", showBackground = true)
@Composable
private fun UtilitiesLoadingDarkPreview() = UtilitiesStatePreview(UtilitiesUiState(isLoading = true), darkTheme = true)

@Preview(name = "empty light", showBackground = true)
@Composable
private fun UtilitiesEmptyPreview() = UtilitiesStatePreview(UtilitiesUiState(isLoading = false, modules = emptyList()))

@Preview(name = "empty dark", showBackground = true)
@Composable
private fun UtilitiesEmptyDarkPreview() = UtilitiesStatePreview(UtilitiesUiState(isLoading = false, modules = emptyList()), darkTheme = true)

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
