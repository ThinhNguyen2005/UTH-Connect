package com.giathinh.uthconnect.presentation.housing

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import coil.compose.AsyncImage
import com.giathinh.uthconnect.R
import com.giathinh.uthconnect.core.designsystem.component.BadgeTone
import com.giathinh.uthconnect.core.designsystem.component.UthCard
import com.giathinh.uthconnect.core.designsystem.component.UthEmptyState
import com.giathinh.uthconnect.core.designsystem.component.UthPrimaryButton
import com.giathinh.uthconnect.core.designsystem.component.UthStatusBadge
import com.giathinh.uthconnect.core.designsystem.theme.UthDimens
import com.giathinh.uthconnect.core.designsystem.theme.UthPreview
import com.giathinh.uthconnect.core.designsystem.theme.UthTheme
import com.giathinh.uthconnect.domain.model.GenderPreference
import com.giathinh.uthconnect.domain.model.HostVerificationMethod
import com.giathinh.uthconnect.domain.model.HousingListing
import com.giathinh.uthconnect.domain.model.HousingListingType
import com.giathinh.uthconnect.domain.model.HousingPriceRange
import com.giathinh.uthconnect.domain.model.RoomAmenity
import com.giathinh.uthconnect.domain.model.UthCampus
import java.text.NumberFormat
import java.util.Locale

/**
 * Feed Ghép trọ — stateless, nhận [HousingUiState] và phát [HousingAction]
 * (docs/CODING_STANDARDS.md mục 3.2). Header thương hiệu nền xanh chuẩn UTH,
 * thanh tìm kiếm + nút tune lọc, hai hàng chip nhanh, card phòng hiển thị rõ tiêu đề và thông tin.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HousingScreen(
    uiState: HousingUiState,
    onAction: (HousingAction) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    onListingClick: (String) -> Unit = {},
    onCreatePost: () -> Unit = {},
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { HousingHeader(onNavigateBack = onNavigateBack) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCreatePost,
                shape = UthDimens.Radius.PillShape,
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.housing_action_create), fontWeight = FontWeight.Bold) },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            HousingSearchBar(
                query = uiState.searchQuery,
                onSearch = { onAction(HousingAction.Search(it)) },
                onOpenFilters = { onAction(HousingAction.OpenFilters) },
            )
            CampusFilterRow(
                selected = uiState.selectedCampus,
                onSelect = { onAction(HousingAction.SelectCampus(it)) },
            )
            PriceAndGenderFilterRow(
                selectedPriceRange = uiState.priceRange,
                onSelectPriceRange = { onAction(HousingAction.SelectPriceRange(it)) },
                selectedGender = uiState.genderPreference,
                onSelectGender = { onAction(HousingAction.SelectGender(it)) },
            )
            FeedHeader(
                visibleCount = uiState.visibleCount,
                hasActiveFilters = uiState.hasActiveFilters,
                onClearFilters = { onAction(HousingAction.ClearFilters) },
            )
            when {
                uiState.isLoading -> HousingSkeletonList()

                uiState.failure != null -> UthEmptyState(
                    title = stringResource(R.string.housing_load_error_title),
                    body = stringResource(R.string.housing_load_error_body),
                    icon = Icons.Filled.Warning,
                    actionLabel = stringResource(R.string.common_retry),
                    onAction = { onAction(HousingAction.Retry) },
                )

                uiState.isEmptyBecauseOfFilters -> UthEmptyState(
                    title = stringResource(R.string.housing_empty_filtered_title),
                    body = stringResource(R.string.housing_empty_filtered_body),
                    icon = Icons.Filled.Search,
                    actionLabel = stringResource(R.string.housing_clear_filters),
                    onAction = { onAction(HousingAction.ClearFilters) },
                )

                uiState.isEmpty -> UthEmptyState(
                    title = stringResource(R.string.housing_empty_title),
                    body = stringResource(R.string.housing_empty_body),
                    icon = Icons.Filled.Search,
                )

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = UthDimens.Spacing.Lg,
                        end = UthDimens.Spacing.Lg,
                        top = UthDimens.Spacing.Sm,
                        bottom = UthDimens.Size.ButtonHeight + UthDimens.Spacing.Lg,
                    ),
                    verticalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Md),
                ) {
                    items(items = uiState.filteredListings, key = { it.id }) { listing ->
                        HousingListingCard(
                            listing = listing,
                            onReport = { onAction(HousingAction.OpenReport(listing.id)) },
                            onClick = { onListingClick(listing.id) },
                        )
                    }
                }
            }
        }
    }

    if (uiState.isFilterDialogOpen) {
        HousingFilterBottomSheet(uiState = uiState, onAction = onAction)
    }
    if (uiState.isReportDialogOpen) {
        HousingReportBottomSheet(selectedReason = uiState.reportingReason, onAction = onAction)
    }
}

/** Header nền xanh thương hiệu UTH: nút Back + Logo UTH + tiêu đề Ghép trọ thu gọn trên một hàng. */
@Composable
private fun HousingHeader(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .background(colors.primary)
            .drawBehind {
                val ringRadius = size.width * UthDimens.Layout.HomeRingRadiusFraction
                val ringStroke = size.width * UthDimens.Layout.HomeRingStrokeFraction
                drawCircle(
                    color = colors.onPrimary.copy(alpha = UthDimens.Opacity.HomeHeaderRing),
                    radius = ringRadius,
                    center = Offset(size.width + ringRadius * UthDimens.Layout.HomeRingOffsetFraction, size.height),
                    style = Stroke(width = ringStroke),
                )
                drawCircle(
                    color = colors.secondary.copy(alpha = UthDimens.Opacity.HomeHeaderAccentRing),
                    radius = ringRadius * UthDimens.Layout.HomeAccentRingRadiusFactor,
                    center = Offset(size.width * UthDimens.Layout.HomeAccentRingCenterFraction, 0f),
                    style = Stroke(width = ringStroke * UthDimens.Layout.HomeAccentRingStrokeFactor),
                )
            }
            .padding(
                start = UthDimens.Spacing.Sm,
                end = UthDimens.Spacing.Lg,
                top = UthDimens.Spacing.Sm,
                bottom = UthDimens.Spacing.Sm,
            ),
    ) {
        IconButton(
            onClick = onNavigateBack,
            modifier = Modifier.size(UthDimens.Size.Avatar),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.nav_back),
                tint = colors.onPrimary,
            )
        }
        Spacer(Modifier.width(UthDimens.Spacing.Sm))
        Box(
            modifier = Modifier
                .size(UthDimens.Size.Avatar)
                .clip(MaterialTheme.shapes.small)
                .background(colors.onPrimary),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.home_logo_abbr),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = colors.primary,
            )
        }
        Spacer(Modifier.width(UthDimens.Spacing.Md))
        Text(
            text = stringResource(R.string.housing_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = colors.onPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun HousingSearchBar(
    query: String,
    onSearch: (String) -> Unit,
    onOpenFilters: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = UthDimens.Spacing.Lg, vertical = UthDimens.Spacing.Sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Sm),
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onSearch,
            placeholder = {
                Text(stringResource(R.string.housing_search_hint), style = MaterialTheme.typography.bodyMedium)
            },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onSearch("") }) {
                        Icon(
                            imageVector = Icons.Filled.Clear,
                            contentDescription = stringResource(R.string.housing_search_clear),
                        )
                    }
                }
            },
            modifier = Modifier.weight(1f),
            shape = MaterialTheme.shapes.medium,
            singleLine = true,
        )
        Surface(
            onClick = onOpenFilters,
            modifier = Modifier.size(UthDimens.Size.ButtonHeight),
            shape = MaterialTheme.shapes.medium,
            color = UthTheme.extendedColors.surfaceSubtle,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(R.drawable.ic_housing_filter),
                    contentDescription = stringResource(R.string.housing_filter_trigger),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(UthDimens.Size.Icon),
                )
            }
        }
    }
}

@Composable
private fun CampusFilterRow(
    selected: UthCampus?,
    onSelect: (UthCampus?) -> Unit,
    modifier: Modifier = Modifier,
) {
    ChipRow(modifier = modifier) {
        HousingFilterChip(
            label = stringResource(R.string.housing_filter_all),
            selected = selected == null,
            onClick = { onSelect(null) },
        )
        UthCampus.entries.forEach { campus ->
            HousingFilterChip(
                label = stringResource(campus.labelRes()),
                selected = selected == campus,
                onClick = { onSelect(campus) },
            )
        }
    }
}

@Composable
private fun PriceAndGenderFilterRow(
    selectedPriceRange: HousingPriceRange,
    onSelectPriceRange: (HousingPriceRange) -> Unit,
    selectedGender: GenderPreference?,
    onSelectGender: (GenderPreference?) -> Unit,
    modifier: Modifier = Modifier,
) {
    ChipRow(modifier = modifier) {
        HousingPriceRange.entries.forEach { range ->
            HousingFilterChip(
                label = stringResource(range.labelRes()),
                selected = selectedPriceRange == range,
                onClick = { onSelectPriceRange(range) },
            )
        }
        Box(
            modifier = Modifier
                .width(UthDimens.Size.Stroke)
                .height(UthDimens.Spacing.Lg)
                .background(MaterialTheme.colorScheme.outlineVariant),
        )
        HousingFilterChip(
            label = stringResource(R.string.housing_filter_all),
            selected = selectedGender == null,
            onClick = { onSelectGender(null) },
        )
        listOf(GenderPreference.MALE, GenderPreference.FEMALE).forEach { gender ->
            HousingFilterChip(
                label = gender.label(),
                selected = selectedGender == gender,
                onClick = { onSelectGender(gender) },
            )
        }
    }
}

@Composable
private fun ChipRow(modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = UthDimens.Spacing.Lg, vertical = UthDimens.Spacing.Xs),
        horizontalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Sm),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

@Composable
private fun HousingFilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, style = MaterialTheme.typography.labelMedium) },
        leadingIcon = if (selected) {
            {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    modifier = Modifier.size(UthDimens.Size.IconBadge),
                )
            }
        } else {
            null
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
    )
}

@Composable
private fun FeedHeader(
    visibleCount: Int,
    hasActiveFilters: Boolean,
    onClearFilters: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = UthDimens.Spacing.Lg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = pluralStringResource(R.plurals.housing_open_count, visibleCount, visibleCount),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        if (hasActiveFilters) {
            TextButton(onClick = onClearFilters) {
                Text(stringResource(R.string.housing_clear_filters))
            }
        }
    }
}

/** Skeleton khối `surfaceSubtle` khi tải feed. */
@Composable
private fun HousingSkeletonList(modifier: Modifier = Modifier) {
    val loadingLabel = stringResource(R.string.housing_loading)
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(UthDimens.Spacing.Lg)
            .semantics { contentDescription = loadingLabel },
        verticalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Md),
    ) {
        HousingSkeletonCard()
        HousingSkeletonCard()
    }
}

@Composable
private fun HousingSkeletonCard(modifier: Modifier = Modifier) {
    UthCard(modifier = modifier.fillMaxWidth(), contentPadding = false) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(UthDimens.Layout.CoverAspectRatio)
                .background(UthTheme.extendedColors.surfaceSubtle),
        )
        Column(
            modifier = Modifier.padding(UthDimens.Spacing.Lg),
            verticalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Sm),
        ) {
            SkeletonLine(fraction = UthDimens.Layout.HousingSkeletonTitleFraction, height = UthDimens.Size.Icon)
            SkeletonLine(fraction = UthDimens.Layout.HousingSkeletonBodyFraction, height = UthDimens.Size.IconInline)
            SkeletonLine(fraction = UthDimens.Layout.HousingSkeletonBodyFraction, height = UthDimens.Size.IconInline)
        }
    }
}

@Composable
private fun SkeletonLine(fraction: Float, height: Dp, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth(fraction)
            .height(height)
            .clip(MaterialTheme.shapes.extraSmall)
            .background(UthTheme.extendedColors.surfaceSubtle),
    )
}

@Composable
private fun HousingListingCard(
    listing: HousingListing,
    onReport: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    UthCard(onClick = onClick, modifier = modifier.fillMaxWidth(), contentPadding = false) {
        HousingCoverMedia(listing = listing)
        Column(modifier = Modifier.padding(UthDimens.Spacing.Lg)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = listing.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (listing.areaM2 != null && listing.areaM2 > 0) {
                    Spacer(modifier = Modifier.width(UthDimens.Spacing.Sm))
                    Surface(
                        shape = MaterialTheme.shapes.extraSmall,
                        color = UthTheme.extendedColors.surfaceSubtle,
                    ) {
                        Text(
                            text = stringResource(R.string.housing_area_sqm, listing.areaM2),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(
                                horizontal = UthDimens.Spacing.Sm,
                                vertical = UthDimens.Spacing.Xxs,
                            ),
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(UthDimens.Spacing.Xs))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    imageVector = Icons.Filled.LocationOn,
                    contentDescription = null,
                    modifier = Modifier.size(UthDimens.Size.IconInline),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.width(UthDimens.Spacing.Xs))
                Text(
                    text = listing.addressLabel.ifBlank { stringResource(listing.campus.labelRes()) },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (listing.hasLocation) {
                    Text(
                        text = " · ",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
                    Text(
                        text = stringResource(
                            R.string.housing_card_distance,
                            stringResource(listing.campus.labelRes()),
                            distanceText(listing.distanceToCampusM),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Spacer(modifier = Modifier.height(UthDimens.Spacing.Sm))
            VerificationLabel(listing = listing)
            Spacer(modifier = Modifier.height(UthDimens.Spacing.Sm))
            HousingNoteBox(listing = listing)
            Spacer(modifier = Modifier.height(UthDimens.Spacing.Md))
            HousingCardFooter(
                listing = listing,
                onReport = onReport,
            )
        }
    }
}

/** Ảnh bìa 16:9: pill trạng thái, số lượng ảnh, thời gian đăng và giá đè lên scrim đáy. */
@Composable
private fun HousingCoverMedia(
    listing: HousingListing,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(UthDimens.Layout.CoverAspectRatio)
            .clip(MaterialTheme.shapes.medium)
            .background(
                Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.primaryContainer,
                        UthTheme.extendedColors.surfaceSubtle,
                    ),
                ),
            ),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_utilities_housing),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .align(Alignment.Center)
                .size(UthDimens.Size.AvatarLarge),
        )
        AsyncImage(
            model = listing.images.firstOrNull(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Transparent,
                            MaterialTheme.colorScheme.scrim.copy(alpha = UthDimens.Opacity.HousingCoverScrim),
                        ),
                    ),
                ),
        )
        UthStatusBadge(
            text = pluralStringResource(R.plurals.housing_slots, listing.availableSlots, listing.availableSlots),
            tone = BadgeTone.Success,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(UthDimens.Spacing.Sm),
        )
        if (listing.images.size > 1) {
            Surface(
                shape = MaterialTheme.shapes.extraSmall,
                color = MaterialTheme.colorScheme.surface.copy(alpha = UthDimens.Opacity.CoverLabelSurface),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(UthDimens.Spacing.Sm),
            ) {
                Text(
                    text = stringResource(R.string.housing_detail_photo_count, 1, listing.images.size),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(
                        horizontal = UthDimens.Spacing.Sm,
                        vertical = UthDimens.Spacing.Xxs,
                    ),
                )
            }
        }
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(UthDimens.Spacing.Sm),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Surface(
                shape = MaterialTheme.shapes.extraSmall,
                color = MaterialTheme.colorScheme.scrim.copy(alpha = UthDimens.Opacity.AuthOutlineButtonBorder),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = UthDimens.Spacing.Sm, vertical = UthDimens.Spacing.Xxs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = null,
                        tint = UthTheme.extendedColors.headerOnGradient,
                        modifier = Modifier.size(UthDimens.Size.IconBadge),
                    )
                    Spacer(Modifier.width(UthDimens.Spacing.Xs))
                    Text(
                        text = stringResource(R.string.housing_posted_today, "10:30"),
                        style = MaterialTheme.typography.labelSmall,
                        color = UthTheme.extendedColors.headerOnGradient,
                    )
                }
            }
            Text(
                text = stringResource(R.string.housing_price_per_month, formatVnd(listing.monthlyRentVnd)),
                style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
    }
}

@Composable
private fun HousingNoteBox(listing: HousingListing, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = UthTheme.extendedColors.surfaceSubtle,
    ) {
        Row(
            modifier = Modifier.padding(UthDimens.Spacing.Md),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Sm),
        ) {
            Icon(
                imageVector = Icons.Filled.Person,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .size(UthDimens.Size.IconInline)
                    .padding(top = UthDimens.Spacing.Xxs),
            )
            Column {
                Text(
                    text = stringResource(R.string.housing_roommate_heading, listing.genderPreference.label()),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                val amenityLabels = listing.amenities.map { stringResource(it.labelRes()) }
                val noteText = if (listing.scheduleNote.isNotBlank()) {
                    listing.scheduleNote
                } else if (amenityLabels.isNotEmpty()) {
                    amenityLabels.joinToString(", ")
                } else {
                    stringResource(R.string.housing_detail_schedule_title)
                }
                Text(
                    text = noteText,
                    style = MaterialTheme.typography.bodySmall,
                    color = UthTheme.extendedColors.textSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun HousingCardFooter(
    listing: HousingListing,
    onReport: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Sm),
    ) {
        InitialsAvatar(name = listing.hostName)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = listing.hostName,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (listing.hostStudentId.isNotBlank()) {
                Text(
                    text = stringResource(R.string.housing_detail_host_id, listing.hostStudentId),
                    style = MaterialTheme.typography.bodySmall,
                    color = UthTheme.extendedColors.textSecondary,
                    maxLines = 1,
                )
            }
        }
        IconButton(
            onClick = onReport,
            modifier = Modifier.size(UthDimens.Size.MinTouchTarget),
        ) {
            Icon(
                imageVector = Icons.Filled.Warning,
                contentDescription = stringResource(R.string.housing_report),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(UthDimens.Size.IconInline),
            )
        }
    }
}

/** Avatar chữ cái đầu tên chủ tin. */
@Composable
private fun InitialsAvatar(name: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.size(UthDimens.Size.Avatar),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(text = initialsOf(name), style = MaterialTheme.typography.labelMedium)
        }
    }
}

/**
 * Badge xác thực hiện trên mọi card (tiêu chí nghiệm thu README 3.2).
 */
@Composable
private fun VerificationLabel(listing: HousingListing, modifier: Modifier = Modifier) {
    if (listing.verified) {
        UthStatusBadge(
            text = stringResource(R.string.housing_verified_student_badge),
            tone = BadgeTone.Info,
            modifier = modifier,
        )
    } else {
        Text(
            text = stringResource(R.string.housing_unverified),
            modifier = modifier,
            style = MaterialTheme.typography.labelSmall,
            color = UthTheme.extendedColors.textSecondary,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun HousingFilterBottomSheet(uiState: HousingUiState, onAction: (HousingAction) -> Unit) {
    ModalBottomSheet(
        onDismissRequest = { onAction(HousingAction.CloseFilters) },
        shape = MaterialTheme.shapes.large,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = UthDimens.Spacing.Lg)
                .padding(bottom = UthDimens.Spacing.Xl),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.housing_filter_dialog_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (uiState.hasActiveFilters) {
                    TextButton(
                        onClick = { onAction(HousingAction.ClearFilters) },
                    ) {
                        Text(
                            text = stringResource(R.string.housing_clear_filters),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }

            Spacer(Modifier.height(UthDimens.Spacing.Md))

            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Lg),
            ) {
                FilterGroup(label = stringResource(R.string.housing_filter_campus_label)) {
                    HousingFilterChip(
                        label = stringResource(R.string.housing_filter_all),
                        selected = uiState.selectedCampus == null,
                        onClick = { onAction(HousingAction.SelectCampus(null)) },
                    )
                    UthCampus.entries.forEach { campus ->
                        HousingFilterChip(
                            label = stringResource(campus.labelRes()),
                            selected = uiState.selectedCampus == campus,
                            onClick = { onAction(HousingAction.SelectCampus(campus)) },
                        )
                    }
                }
                FilterGroup(label = stringResource(R.string.housing_filter_price_range_label)) {
                    HousingPriceRange.entries.forEach { range ->
                        HousingFilterChip(
                            label = stringResource(range.labelRes()),
                            selected = uiState.priceRange == range,
                            onClick = { onAction(HousingAction.SelectPriceRange(range)) },
                        )
                    }
                }
                FilterGroup(label = stringResource(R.string.housing_filter_rent_label)) {
                    HousingFilterChip(
                        label = stringResource(R.string.housing_filter_all),
                        selected = uiState.maxRentVnd == null,
                        onClick = { onAction(HousingAction.SelectMaxRent(null)) },
                    )
                    RentPresets.forEach { rent ->
                        HousingFilterChip(
                            label = stringResource(R.string.housing_filter_rent_option, formatVnd(rent)),
                            selected = uiState.maxRentVnd == rent,
                            onClick = { onAction(HousingAction.SelectMaxRent(rent)) },
                        )
                    }
                }
                FilterGroup(label = stringResource(R.string.housing_filter_distance_label)) {
                    HousingFilterChip(
                        label = stringResource(R.string.housing_filter_all),
                        selected = uiState.maxDistanceM == null,
                        onClick = { onAction(HousingAction.SelectMaxDistance(null)) },
                    )
                    DistancePresetsM.forEach { distanceM ->
                        val label = if (distanceM >= 1_000) {
                            stringResource(R.string.housing_filter_distance_option_km, distanceM / 1_000)
                        } else {
                            stringResource(R.string.housing_filter_distance_option_m, distanceM)
                        }
                        HousingFilterChip(
                            label = label,
                            selected = uiState.maxDistanceM == distanceM,
                            onClick = { onAction(HousingAction.SelectMaxDistance(distanceM)) },
                        )
                    }
                }
                FilterGroup(label = stringResource(R.string.housing_filter_gender_label)) {
                    HousingFilterChip(
                        label = stringResource(R.string.housing_filter_all),
                        selected = uiState.genderPreference == null,
                        onClick = { onAction(HousingAction.SelectGender(null)) },
                    )
                    listOf(GenderPreference.MALE, GenderPreference.FEMALE).forEach { gender ->
                        HousingFilterChip(
                            label = gender.label(),
                            selected = uiState.genderPreference == gender,
                            onClick = { onAction(HousingAction.SelectGender(gender)) },
                        )
                    }
                }
                FilterGroup(label = stringResource(R.string.housing_filter_sort_label)) {
                    HousingSort.entries.forEach { sort ->
                        HousingFilterChip(
                            label = stringResource(sort.labelRes()),
                            selected = uiState.sort == sort,
                            onClick = { onAction(HousingAction.SelectSort(sort)) },
                        )
                    }
                }
            }

            Spacer(Modifier.height(UthDimens.Spacing.Lg))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Md),
            ) {
                OutlinedButton(
                    onClick = {
                        onAction(HousingAction.ClearFilters)
                        onAction(HousingAction.CloseFilters)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = UthDimens.Size.ButtonHeight),
                    shape = MaterialTheme.shapes.small,
                ) {
                    Text(stringResource(R.string.housing_clear_filters))
                }
                UthPrimaryButton(
                    text = stringResource(R.string.housing_filter_done),
                    onClick = { onAction(HousingAction.CloseFilters) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterGroup(label: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Xs)) {
        Text(text = label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Sm),
            verticalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Xs),
        ) {
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HousingReportBottomSheet(selectedReason: HousingReportReason?, onAction: (HousingAction) -> Unit) {
    ModalBottomSheet(
        onDismissRequest = { onAction(HousingAction.CloseReport) },
        shape = MaterialTheme.shapes.large,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = UthDimens.Spacing.Lg)
                .padding(bottom = UthDimens.Spacing.Xl),
        ) {
            Text(
                text = stringResource(R.string.housing_report_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.housing_report_body),
                style = MaterialTheme.typography.bodyMedium,
                color = UthTheme.extendedColors.textSecondary,
                modifier = Modifier.padding(top = UthDimens.Spacing.Xs),
            )

            Spacer(Modifier.height(UthDimens.Spacing.Lg))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectableGroup(),
                verticalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Xs),
            ) {
                HousingReportReason.entries.forEach { reason ->
                    val isSelected = selectedReason == reason
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.small)
                            .selectable(
                                selected = isSelected,
                                onClick = { onAction(HousingAction.SelectReportReason(reason)) },
                                role = Role.RadioButton,
                            )
                            .padding(vertical = UthDimens.Spacing.Sm, horizontal = UthDimens.Spacing.Xs),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Sm),
                    ) {
                        RadioButton(selected = isSelected, onClick = null)
                        Text(
                            text = stringResource(reason.labelRes()),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }

            Spacer(Modifier.height(UthDimens.Spacing.Xl))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Md),
            ) {
                OutlinedButton(
                    onClick = { onAction(HousingAction.CloseReport) },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = UthDimens.Size.ButtonHeight),
                    shape = MaterialTheme.shapes.small,
                ) {
                    Text(stringResource(R.string.housing_report_cancel))
                }
                UthPrimaryButton(
                    text = stringResource(R.string.housing_report_submit),
                    onClick = { onAction(HousingAction.ConfirmReport) },
                    enabled = selectedReason != null,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun distanceText(distanceM: Int): String = if (distanceM >= 1_000) {
    stringResource(R.string.housing_distance_km, distanceM / 1_000f)
} else {
    stringResource(R.string.housing_distance_m, distanceM)
}

@Composable
private fun GenderPreference.label(): String = when (this) {
    GenderPreference.ANY -> stringResource(R.string.housing_gender_any)
    GenderPreference.MALE -> stringResource(R.string.housing_gender_male)
    GenderPreference.FEMALE -> stringResource(R.string.housing_gender_female)
}

@StringRes
fun HousingListingType.labelRes(): Int = when (this) {
    HousingListingType.ROOM_FOR_RENT -> R.string.housing_type_room_for_rent
    HousingListingType.LOOKING_FOR_ROOMMATE -> R.string.housing_type_looking_for_roommate
    HousingListingType.LOOKING_FOR_ROOM -> R.string.housing_type_looking_for_room
}

@StringRes
fun UthCampus.labelRes(): Int = when (this) {
    UthCampus.CS1_QUANG_TRUNG -> R.string.housing_campus_cs1
    UthCampus.CS2_THU_DUC -> R.string.housing_campus_cs2
    UthCampus.CS3_TO_KY -> R.string.housing_campus_cs3
}

@StringRes
fun HousingSort.labelRes(): Int = when (this) {
    HousingSort.NEWEST -> R.string.housing_sort_newest
    HousingSort.NEAREST_CAMPUS -> R.string.housing_sort_nearest
    HousingSort.CHEAPEST -> R.string.housing_sort_cheapest
}

@StringRes
fun HousingReportReason.labelRes(): Int = when (this) {
    HousingReportReason.FAKE_INFO -> R.string.housing_report_reason_fake
    HousingReportReason.BROKER_SPAM -> R.string.housing_report_reason_broker
    HousingReportReason.WRONG_PRICE -> R.string.housing_report_reason_wrong_price
    HousingReportReason.ALREADY_FULL -> R.string.housing_report_reason_full
    HousingReportReason.OFFENSIVE_CONTENT -> R.string.housing_report_reason_offensive
}

@StringRes
fun HousingPriceRange.labelRes(): Int = when (this) {
    HousingPriceRange.ALL -> R.string.housing_price_all
    HousingPriceRange.UNDER_2M -> R.string.housing_price_under_2m
    HousingPriceRange.FROM_2M_TO_3_5M -> R.string.housing_price_2m_to_3_5m
    HousingPriceRange.OVER_3_5M -> R.string.housing_price_over_3_5m
}

@StringRes
fun RoomAmenity.labelRes(): Int = when (this) {
    RoomAmenity.MEZZANINE -> R.string.housing_amenity_mezzanine
    RoomAmenity.AIR_CONDITIONER -> R.string.housing_amenity_air_conditioner
    RoomAmenity.MACHINE_WASH -> R.string.housing_amenity_machine_wash
    RoomAmenity.FREE_HOURS -> R.string.housing_amenity_free_hours
    RoomAmenity.GOVERNMENT_UTILITY_RATES -> R.string.housing_amenity_government_utility_rates
    RoomAmenity.SECURED_PARKING -> R.string.housing_amenity_secured_parking
    RoomAmenity.COOKING -> R.string.housing_amenity_cooking
    RoomAmenity.HIGH_SPEED_WIFI -> R.string.housing_amenity_wifi
    RoomAmenity.HOT_WATER -> R.string.housing_amenity_hot_water
    RoomAmenity.BALCONY -> R.string.housing_amenity_balcony
    RoomAmenity.KEY_CARD -> R.string.housing_amenity_key_card
}

private fun formatVnd(amount: Int): String {
    val formatter = NumberFormat.getIntegerInstance(Locale.forLanguageTag("vi-VN"))
    return formatter.format(amount)
}

private fun initialsOf(name: String): String {
    val tokens = name.trim().split("\\s+".toRegex()).filter { it.isNotEmpty() }
    return when {
        tokens.isEmpty() -> "?"
        tokens.size == 1 -> tokens.first().take(2).uppercase()
        else -> (tokens.first().take(1) + tokens.last().take(1)).uppercase()
    }
}

private val RentPresets = listOf(1_500_000, 2_000_000, 2_500_000, 3_000_000)
private val DistancePresetsM = listOf(500, 1_000, 2_000, 3_000)

@Preview
@Composable
private fun HousingScreenContentPreview() {
    UthPreview {
        HousingScreen(
            uiState = HousingUiState(
                listings = listOf(
                    HousingListing(
                        id = "PREVIEW-1",
                        type = HousingListingType.ROOM_FOR_RENT,
                        campus = UthCampus.CS1_QUANG_TRUNG,
                        title = "Phòng gác lửng gần CS1, giờ giấc tự do, có máy lạnh",
                        description = "Phòng đẹp sạch sẽ.",
                        hostName = "Trần Hưng",
                        hostStudentId = "2151050012",
                        monthlyRentVnd = 2_800_000,
                        depositVnd = 2_000_000,
                        utilitiesIncluded = false,
                        availableSlots = 1,
                        genderPreference = GenderPreference.MALE,
                        scheduleNote = "Giờ giấc tự do, có máy lạnh, nấu ăn riêng.",
                        addressLabel = "Khu vực Đường D2 (Nguyễn Gia Trí)",
                        approxLatitude = 10.8,
                        approxLongitude = 106.6,
                        distanceToCampusM = 1_200,
                        verified = true,
                        areaM2 = 22,
                    ),
                ),
            ),
            onAction = {},
            onNavigateBack = {},
        )
    }
}
