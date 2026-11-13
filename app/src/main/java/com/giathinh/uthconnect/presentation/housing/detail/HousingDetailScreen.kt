package com.giathinh.uthconnect.presentation.housing.detail

import android.animation.ValueAnimator
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
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
import com.giathinh.uthconnect.domain.model.HousingListing
import com.giathinh.uthconnect.domain.model.HousingListingStatus
import com.giathinh.uthconnect.domain.model.HousingListingType
import com.giathinh.uthconnect.domain.model.RoomAmenity
import com.giathinh.uthconnect.domain.model.UthCampus
import java.text.NumberFormat
import java.util.Locale

/**
 * Màn chi tiết tin trọ — tuân thủ Design System (core/designsystem).
 * TopBar đồng bộ với trang Ghép trọ (nền Navy UTH, ambient rings, logo UTH),
 * các danh mục tiện ích sử dụng đồng bộ icon vector drawable `ic_amenity_*`,
 * các thẻ thông tin bọc `UthCard` chuẩn token M3.
 */
@Composable
fun HousingDetailScreen(
    uiState: HousingDetailUiState,
    onAction: (HousingDetailAction) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listing = uiState.listing
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            HousingDetailHeader(
                title = stringResource(R.string.housing_detail_title),
                onNavigateBack = onNavigateBack,
            )
        },
        bottomBar = {
            if (listing != null) {
                DetailBottomBar(
                    listing = listing,
                    onCall = { onAction(HousingDetailAction.CallHost) },
                    onZalo = { onAction(HousingDetailAction.OpenZalo) },
                )
            }
        },
    ) { innerPadding ->
        when {
            uiState.isLoading -> {
                HousingDetailSkeleton(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                )
            }

            uiState.failure == HousingDetailFailure.NotFound || uiState.isEmpty -> {
                UthEmptyState(
                    title = stringResource(R.string.housing_detail_not_found_title),
                    body = stringResource(R.string.housing_detail_not_found_body),
                    icon = Icons.Filled.Home,
                    actionLabel = stringResource(R.string.common_back),
                    onAction = onNavigateBack,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                )
            }

            uiState.failure != null -> {
                UthEmptyState(
                    title = stringResource(R.string.housing_load_error_title),
                    body = stringResource(R.string.housing_load_error_body),
                    icon = Icons.Filled.Warning,
                    actionLabel = stringResource(R.string.common_retry),
                    onAction = { onAction(HousingDetailAction.Retry) },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                )
            }

            listing != null -> {
                HousingDetailContent(
                    listing = listing,
                    isOwner = uiState.isOwner,
                    isActionLoading = uiState.isActionLoading,
                    onOpenMap = { onAction(HousingDetailAction.OpenMap) },
                    onMarkFulfilled = { onAction(HousingDetailAction.MarkFulfilled) },
                    onDelete = { onAction(HousingDetailAction.Delete) },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                )
            }
        }
    }
}

/**
 * TopBar đồng bộ với trang Ghép trọ & Đăng tin:
 * Nền Navy UTH, họa tiết vòng tròn ambient ring, nút Back, logo UTH và tiêu đề.
 */
@Composable
private fun HousingDetailHeader(
    title: String,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    trailingAction: @Composable (() -> Unit)? = null,
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
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = colors.onPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        trailingAction?.invoke()
    }
}

@Composable
private fun HousingDetailContent(
    listing: HousingListing,
    isOwner: Boolean,
    isActionLoading: Boolean,
    onOpenMap: () -> Unit,
    onMarkFulfilled: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(
            start = UthDimens.Spacing.Lg,
            end = UthDimens.Spacing.Lg,
            top = UthDimens.Spacing.Sm,
            bottom = UthDimens.Spacing.Xxl,
        ),
        verticalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Lg),
    ) {
        item {
            DetailGallery(listing = listing)
        }

        item {
            PrimaryInfoCard(listing = listing)
        }

        item {
            HostCard(listing = listing)
        }

        if (listing.amenities.isNotEmpty()) {
            item {
                AmenitiesCard(amenities = listing.amenities)
            }
        }

        if (listing.description.isNotBlank()) {
            item {
                HousingSectionCard(
                    title = stringResource(R.string.housing_detail_description_heading),
                    body = listing.description,
                )
            }
        }

        if (listing.scheduleNote.isNotBlank()) {
            item {
                HousingSectionCard(
                    title = stringResource(R.string.housing_detail_schedule_title),
                    body = listing.scheduleNote,
                )
            }
        }

        item {
            MapCard(listing = listing, onOpenMap = onOpenMap)
        }

        if (isOwner) {
            item {
                OwnerActionsCard(
                    listing = listing,
                    isActionLoading = isActionLoading,
                    onMarkFulfilled = onMarkFulfilled,
                    onDelete = onDelete,
                )
            }
        }
    }
}

/**
 * Khối quản lý chỉ hiện với chủ tin: đánh dấu hết phòng để nhả suất đăng (backend
 * giới hạn 2 tin mở) và xóa tin vĩnh viễn. Trước đây action MarkFulfilled đã có
 * nhưng không nút nào gọi — chủ tin kẹt ở trần 2 tin không lối thoát.
 */
@Composable
private fun OwnerActionsCard(
    listing: HousingListing,
    isActionLoading: Boolean,
    onMarkFulfilled: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    UthCard(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.housing_detail_manage_title).uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = UthTheme.extendedColors.textSecondary,
        )
        Spacer(modifier = Modifier.height(UthDimens.Spacing.Md))
        if (listing.status == HousingListingStatus.OPEN) {
            FilledTonalButton(
                onClick = onMarkFulfilled,
                enabled = !isActionLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = UthDimens.Size.ButtonHeight),
                shape = MaterialTheme.shapes.small,
            ) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    modifier = Modifier.size(UthDimens.Size.IconInline),
                )
                Spacer(modifier = Modifier.width(UthDimens.Spacing.Sm))
                Text(stringResource(R.string.housing_detail_mark_fulfilled))
            }
            Spacer(modifier = Modifier.height(UthDimens.Spacing.Sm))
        }
        OutlinedButton(
            onClick = { showDeleteDialog = true },
            enabled = !isActionLoading,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = UthDimens.Size.ButtonHeight),
            shape = MaterialTheme.shapes.small,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
        ) {
            Icon(
                imageVector = Icons.Filled.Delete,
                contentDescription = null,
                modifier = Modifier.size(UthDimens.Size.IconInline),
            )
            Spacer(modifier = Modifier.width(UthDimens.Spacing.Sm))
            Text(stringResource(R.string.housing_detail_delete))
        }
    }
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.housing_detail_delete_dialog_title)) },
            text = { Text(stringResource(R.string.housing_detail_delete_dialog_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDelete()
                    },
                ) {
                    Text(
                        text = stringResource(R.string.housing_detail_delete_confirm),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.housing_report_cancel))
                }
            },
        )
    }
}

/**
 * Gallery vuốt ngang tỉ lệ 4:3 theo mockup:
 * Pill trạng thái phòng góc trên trái, đếm ảnh "n/total" góc dưới phải và chấm trang bên dưới.
 */
@Composable
private fun DetailGallery(listing: HousingListing, modifier: Modifier = Modifier) {
    val images = listing.images
    val pageCount = images.size.coerceAtLeast(1)
    val pagerState = rememberPagerState(pageCount = { pageCount })
    val isOpen = listing.status == HousingListingStatus.OPEN

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Sm),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(UthDimens.Layout.HousingGalleryAspectRatio)
                .clip(MaterialTheme.shapes.medium),
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
            ) { page ->
                GalleryPage(
                    imageUrl = images.getOrNull(page),
                    showEmptyLabel = images.isEmpty(),
                    alternateTint = page % 2 == 1,
                )
            }

            // Live Status Pill góc trên trái
            Surface(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(UthDimens.Spacing.Md),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shadowElevation = UthDimens.Elevation.Card,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = UthDimens.Spacing.Md, vertical = UthDimens.Spacing.Xs),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Xs),
                ) {
                    Box(
                        modifier = Modifier
                            .size(UthDimens.Spacing.Sm)
                            .clip(CircleShape)
                            .background(
                                if (isOpen) UthTheme.extendedColors.success else MaterialTheme.colorScheme.error,
                            ),
                    )
                    val statusText = if (isOpen) {
                        val genderText = when (listing.genderPreference) {
                            GenderPreference.MALE -> stringResource(R.string.housing_gender_male)
                            GenderPreference.FEMALE -> stringResource(R.string.housing_gender_female)
                            GenderPreference.ANY -> stringResource(R.string.housing_gender_any)
                        }
                        stringResource(R.string.housing_detail_roommate_looking, listing.availableSlots, genderText)
                    } else {
                        stringResource(R.string.housing_report_reason_full)
                    }
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            // Đếm ảnh góc dưới phải
            if (images.isNotEmpty()) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(UthDimens.Spacing.Md),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.85f),
                    contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                ) {
                    Row(
                        modifier = Modifier.padding(
                            horizontal = UthDimens.Spacing.Sm,
                            vertical = UthDimens.Spacing.Xxs,
                        ),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Xs),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_housing_camera),
                            contentDescription = null,
                            modifier = Modifier.size(UthDimens.Size.IconBadge),
                        )
                        Text(
                            text = "${pagerState.currentPage + 1}/${images.size}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }

        if (images.size > 1) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                repeat(images.size) { index ->
                    val isSelected = pagerState.currentPage == index
                    val width = if (isSelected) UthDimens.Size.IndicatorBar else UthDimens.Size.IndicatorDot
                    val color = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outlineVariant
                    }
                    Box(
                        modifier = Modifier
                            .padding(horizontal = UthDimens.Spacing.Xxs)
                            .size(width = width, height = UthDimens.Size.IndicatorDot)
                            .clip(MaterialTheme.shapes.extraSmall)
                            .background(color),
                    )
                }
            }
        }
    }
}

@Composable
private fun GalleryPage(
    imageUrl: String?,
    showEmptyLabel: Boolean,
    alternateTint: Boolean,
    modifier: Modifier = Modifier,
) {
    val gradientStart = if (alternateTint) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        MaterialTheme.colorScheme.primaryContainer
    }
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    listOf(gradientStart, UthTheme.extendedColors.surfaceSubtle),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = imageUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        if (imageUrl.isNullOrBlank()) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Sm),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_utilities_housing),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(UthDimens.Size.AvatarLarge),
                )
                if (showEmptyLabel) {
                    Text(
                        text = stringResource(R.string.housing_detail_no_images),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/** Thẻ thông tin chính: tiêu đề, giá lớn + cọc, địa chỉ và ô khoảng cách tới cơ sở. */
@Composable
private fun PrimaryInfoCard(listing: HousingListing, modifier: Modifier = Modifier) {
    UthCard(modifier = modifier.fillMaxWidth()) {
        Text(
            text = listing.title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(UthDimens.Spacing.Sm))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.housing_detail_rent_label).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = UthTheme.extendedColors.textSecondary,
                )
                Spacer(modifier = Modifier.height(UthDimens.Spacing.Xxs))
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Xs),
                ) {
                    Text(
                        text = formatVnd(listing.monthlyRentVnd),
                        style = MaterialTheme.typography.headlineMedium.copy(fontFeatureSettings = "tnum"),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = stringResource(R.string.housing_price_unit),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = UthDimens.Spacing.Xxs),
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = stringResource(R.string.housing_detail_deposit_label).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = UthTheme.extendedColors.textSecondary,
                )
                Spacer(modifier = Modifier.height(UthDimens.Spacing.Xxs))
                Text(
                    text = stringResource(
                        R.string.housing_detail_deposit_amount,
                        formatVnd(listing.depositVnd),
                    ),
                    style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
        Spacer(modifier = Modifier.height(UthDimens.Spacing.Sm))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        Spacer(modifier = Modifier.height(UthDimens.Spacing.Sm))
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Sm),
        ) {
            Icon(
                imageVector = Icons.Filled.LocationOn,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .size(UthDimens.Size.IconInline)
                    .padding(top = UthDimens.Spacing.Xxs),
            )
            Text(
                text = listing.addressLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(modifier = Modifier.height(UthDimens.Spacing.Sm))
        ProximityBox(listing = listing)
    }
}

/** Ô nhấn khoảng cách tới cơ sở — dữ liệu thật từ `distanceToCampusM`. */
@Composable
private fun ProximityBox(listing: HousingListing, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.06f),
    ) {
        Row(
            modifier = Modifier.padding(UthDimens.Spacing.Md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Md),
        ) {
            Box(
                modifier = Modifier
                    .size(UthDimens.Size.Avatar)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_utilities_campuses),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(UthDimens.Size.Icon),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                if (listing.distanceToCampusM > 0) {
                    Text(
                        text = stringResource(
                            R.string.housing_detail_proximity,
                            stringResource(listing.campus.labelRes()),
                            distanceText(listing.distanceToCampusM),
                        ),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                } else {
                    // Tin chưa gắn tọa độ: chỉ nêu cơ sở, không bịa khoảng cách 0 m.
                    Text(
                        text = stringResource(listing.campus.labelRes()),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = stringResource(R.string.housing_detail_proximity_unknown),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/** Thẻ sinh viên đăng tin: avatar chữ cái + tên + MSSV + badge xác thực UTH. */
@Composable
private fun HostCard(listing: HousingListing, modifier: Modifier = Modifier) {
    UthCard(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.housing_detail_host_title).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = UthTheme.extendedColors.textSecondary,
            )
            UthStatusBadge(
                text = stringResource(R.string.housing_detail_host_badge),
                tone = BadgeTone.Success,
            )
        }
        Spacer(modifier = Modifier.height(UthDimens.Spacing.Md))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Md),
        ) {
            Box(modifier = Modifier.size(UthDimens.Size.AvatarLarge)) {
                InitialsAvatar(
                    name = listing.hostName,
                    modifier = Modifier.fillMaxSize(),
                )
                Box(
                    modifier = Modifier
                        .size(UthDimens.Size.IconInline)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .align(Alignment.BottomEnd),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_utilities_campuses),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(UthDimens.Size.IconBadge),
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = listing.hostName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (listing.hostStudentId.isNotBlank()) {
                    Spacer(modifier = Modifier.height(UthDimens.Spacing.Xxs))
                    Text(
                        text = stringResource(R.string.housing_detail_host_id, listing.hostStudentId),
                        style = MaterialTheme.typography.bodySmall,
                        color = UthTheme.extendedColors.textSecondary,
                    )
                }
            }
        }
    }
}

/** Avatar chữ cái đầu tên chủ tin. */
@Composable
private fun InitialsAvatar(name: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.size(UthDimens.Size.AvatarLarge),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = initialsOf(name),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

/**
 * Lưới tiện ích phòng trọ (2 cột theo mockup) sử dụng đồng bộ toàn bộ icon vector `ic_amenity_*`.
 */
@Composable
private fun AmenitiesCard(amenities: List<RoomAmenity>, modifier: Modifier = Modifier) {
    UthCard(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.housing_detail_amenities_heading),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = stringResource(R.string.housing_detail_amenities_sub, amenities.size),
                style = MaterialTheme.typography.labelSmall,
                color = UthTheme.extendedColors.textSecondary,
            )
        }
        Spacer(modifier = Modifier.height(UthDimens.Spacing.Md))
        Column(verticalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Sm)) {
            amenities.chunked(2).forEach { rowAmenities ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Sm),
                ) {
                    rowAmenities.forEach { amenity ->
                        AmenityTile(amenity = amenity, modifier = Modifier.weight(1f))
                    }
                    if (rowAmenities.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun AmenityTile(amenity: RoomAmenity, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = UthTheme.extendedColors.surfaceSubtle,
    ) {
        Row(
            modifier = Modifier.padding(UthDimens.Spacing.Md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Sm),
        ) {
            Icon(
                painter = painterResource(amenity.iconRes()),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(UthDimens.Size.Icon),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(amenity.labelRes()),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun HousingSectionCard(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
) {
    UthCard(modifier = modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(UthDimens.Spacing.Sm))
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = MaterialTheme.typography.bodyMedium.lineHeight,
        )
    }
}

/** Khung vị trí tương quan và nút chỉ đường đến cơ sở. */
@Composable
private fun MapCard(listing: HousingListing, onOpenMap: () -> Unit, modifier: Modifier = Modifier) {
    val mapPreviewDescription = stringResource(R.string.housing_detail_map_preview)
    UthCard(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.housing_detail_map_heading),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(UthDimens.Spacing.Md))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(UthDimens.Size.MapPreviewHeight)
                .clip(MaterialTheme.shapes.medium)
                .background(
                    Brush.linearGradient(
                        listOf(
                            UthTheme.extendedColors.surfaceSubtle,
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        ),
                    ),
                )
                .semantics { contentDescription = mapPreviewDescription },
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(UthDimens.Spacing.Lg),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Xs),
            ) {
                Icon(
                    imageVector = Icons.Filled.Place,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(UthDimens.Size.Icon),
                )
                Text(
                    text = listing.addressLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = UthTheme.extendedColors.textSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                )
            }
            UthPrimaryButton(
                // Đích chỉ đường là phòng trọ đang xem (tọa độ/địa chỉ của tin), không phải cơ sở.
                text = stringResource(R.string.housing_detail_directions),
                onClick = onOpenMap,
                leadingIcon = Icons.Filled.Place,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(UthDimens.Spacing.Sm),
            )
        }
    }
}

/** Thanh liên hệ cố định: Gọi điện (nút cong pill) + Nhắn Zalo (primary pill) và cam kết an toàn. */
@Composable
private fun DetailBottomBar(
    listing: HousingListing,
    onCall: () -> Unit,
    onZalo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shadowElevation = UthDimens.Elevation.BottomBar,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = UthDimens.Spacing.Lg, vertical = UthDimens.Spacing.Sm),
            verticalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Sm),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    onClick = onCall,
                    enabled = listing.contactPhone.isNotBlank(),
                    shape = CircleShape,
                    color = UthTheme.extendedColors.surfaceSubtle,
                    modifier = Modifier
                        .weight(1f)
                        .height(UthDimens.Size.ButtonHeight),
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Phone,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(UthDimens.Size.IconInline),
                        )
                        Spacer(modifier = Modifier.width(UthDimens.Spacing.Sm))
                        Text(
                            text = stringResource(R.string.housing_detail_call_now),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }

                // Chủ tin tắt Zalo thì ẩn hẳn nút — không fallback SĐT trái ý chủ tin.
                if (listing.contactZaloEnabled) {
                    Surface(
                        onClick = onZalo,
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier
                            .weight(1f)
                            .height(UthDimens.Size.ButtonHeight),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_housing_notes),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(UthDimens.Size.IconInline),
                            )
                            Spacer(modifier = Modifier.width(UthDimens.Spacing.Sm))
                            Text(
                                text = stringResource(R.string.housing_detail_message_zalo),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                        }
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Xs),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_verified_shield),
                    contentDescription = null,
                    tint = UthTheme.extendedColors.success,
                    modifier = Modifier.size(UthDimens.Size.IconBadge),
                )
                Text(
                    text = stringResource(R.string.housing_detail_privacy_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/**
 * Skeleton trạng thái tải (mục 4.2 Design-system): gallery + khối thông tin chính + khối chủ tin
 * đúng cấu trúc nội dung thật để layout không nhảy khi dữ liệu về, cộng shimmer
 * `Motion.ShimmerMillis`; hệ thống tắt animation thì skeleton đứng yên.
 */
@Composable
private fun HousingDetailSkeleton(modifier: Modifier = Modifier) {
    val loadingLabel = stringResource(R.string.housing_loading)
    val shimmerProgress = if (ValueAnimator.areAnimatorsEnabled()) {
        val shimmer = rememberInfiniteTransition(label = "housingDetailSkeleton")
        val progress by shimmer.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = UthDimens.Motion.ShimmerMillis,
                    easing = LinearEasing,
                ),
                repeatMode = RepeatMode.Restart,
            ),
            label = "housingDetailSkeletonProgress",
        )
        progress
    } else {
        null
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(UthDimens.Spacing.Lg)
            .semantics { contentDescription = loadingLabel },
        verticalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Md),
    ) {
        HousingSkeletonBlock(
            progress = shimmerProgress,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(UthDimens.Layout.HousingGalleryAspectRatio),
        )
        DetailPrimaryInfoSkeleton(progress = shimmerProgress)
        DetailHostSkeleton(progress = shimmerProgress)
    }
}

/** Khối giữ chỗ cho [PrimaryInfoCard]: tiêu đề, hàng giá/cọc, địa chỉ và hộp khoảng cách. */
@Composable
private fun DetailPrimaryInfoSkeleton(progress: Float?, modifier: Modifier = Modifier) {
    UthCard(modifier = modifier.fillMaxWidth()) {
        HousingSkeletonBlock(
            progress = progress,
            modifier = Modifier
                .fillMaxWidth(UthDimens.Layout.HousingSkeletonTitleFraction)
                .height(UthDimens.Size.Icon),
        )
        Spacer(modifier = Modifier.height(UthDimens.Spacing.Sm))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
        ) {
            HousingSkeletonBlock(
                progress = progress,
                modifier = Modifier
                    .weight(1f)
                    .height(UthDimens.Size.Icon),
            )
            Spacer(modifier = Modifier.width(UthDimens.Spacing.Md))
            HousingSkeletonBlock(
                progress = progress,
                modifier = Modifier
                    .fillMaxWidth(UthDimens.Layout.HousingSkeletonBadgeFraction)
                    .height(UthDimens.Size.IconInline),
            )
        }
        Spacer(modifier = Modifier.height(UthDimens.Spacing.Sm))
        HousingSkeletonBlock(
            progress = progress,
            modifier = Modifier
                .fillMaxWidth()
                .height(UthDimens.Size.Stroke),
        )
        Spacer(modifier = Modifier.height(UthDimens.Spacing.Sm))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Sm),
        ) {
            HousingSkeletonBlock(progress = progress, modifier = Modifier.size(UthDimens.Size.IconInline))
            HousingSkeletonBlock(
                progress = progress,
                modifier = Modifier
                    .weight(1f)
                    .height(UthDimens.Size.IconInline),
            )
        }
        Spacer(modifier = Modifier.height(UthDimens.Spacing.Sm))
        HousingSkeletonBlock(
            progress = progress,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier
                .fillMaxWidth()
                .height(UthDimens.Layout.HousingSkeletonInsetBoxHeight),
        )
    }
}

/** Khối giữ chỗ cho [HostCard]: nhãn + badge, avatar lớn và tên/MSSV chủ tin. */
@Composable
private fun DetailHostSkeleton(progress: Float?, modifier: Modifier = Modifier) {
    UthCard(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HousingSkeletonBlock(
                progress = progress,
                modifier = Modifier
                    .fillMaxWidth(UthDimens.Layout.HousingSkeletonBadgeFraction)
                    .height(UthDimens.Size.IconInline),
            )
            HousingSkeletonBlock(
                progress = progress,
                modifier = Modifier
                    .width(UthDimens.Layout.HousingSkeletonChipWidth)
                    .height(UthDimens.Size.Badge),
            )
        }
        Spacer(modifier = Modifier.height(UthDimens.Spacing.Md))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Md),
        ) {
            HousingSkeletonBlock(
                progress = progress,
                shape = CircleShape,
                modifier = Modifier.size(UthDimens.Size.AvatarLarge),
            )
            Column(modifier = Modifier.weight(1f)) {
                HousingSkeletonBlock(
                    progress = progress,
                    modifier = Modifier
                        .fillMaxWidth(UthDimens.Layout.HousingSkeletonTitleFraction)
                        .height(UthDimens.Size.Icon),
                )
                Spacer(modifier = Modifier.height(UthDimens.Spacing.Xxs))
                HousingSkeletonBlock(
                    progress = progress,
                    modifier = Modifier
                        .fillMaxWidth(UthDimens.Layout.HousingSkeletonBodyFraction)
                        .height(UthDimens.Size.IconInline),
                )
            }
        }
    }
}

/**
 * Khối giữ chỗ có shimmer (mục 4.2): nền `surfaceSubtle` cộng dải sáng `surface` chạy ngang,
 * cả hai vẽ trong [drawBehind] để nền không che dải sáng. [progress] null khi hệ thống
 * tắt chuyển động — chỉ vẽ nền tĩnh.
 */
@Composable
private fun HousingSkeletonBlock(
    progress: Float?,
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.extraSmall,
) {
    val base = UthTheme.extendedColors.surfaceSubtle
    val highlight = MaterialTheme.colorScheme.surface.copy(alpha = UthDimens.Opacity.ShimmerHighlight)
    Box(
        modifier = modifier
            .clip(shape)
            .drawBehind {
                drawRect(color = base)
                if (progress != null) {
                    val band = size.width * UthDimens.Layout.ShimmerBandFraction
                    val travel = size.width + band * 2
                    val start = -band + travel * progress
                    drawRect(
                        brush = Brush.linearGradient(
                            colors = listOf(Color.Transparent, highlight, Color.Transparent),
                            start = Offset(start, 0f),
                            end = Offset(start + band, size.height),
                        ),
                    )
                }
            },
    )
}

@StringRes
private fun UthCampus.labelRes(): Int = when (this) {
    UthCampus.CS1_QUANG_TRUNG -> R.string.housing_campus_cs1
    UthCampus.CS2_THU_DUC -> R.string.housing_campus_cs2
    UthCampus.CS3_TO_KY -> R.string.housing_campus_cs3
}

@StringRes
private fun RoomAmenity.labelRes(): Int = when (this) {
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

@DrawableRes
private fun RoomAmenity.iconRes(): Int = when (this) {
    RoomAmenity.MEZZANINE -> R.drawable.ic_amenity_mezzanine
    RoomAmenity.AIR_CONDITIONER -> R.drawable.ic_amenity_ac
    RoomAmenity.MACHINE_WASH -> R.drawable.ic_amenity_washing
    RoomAmenity.FREE_HOURS -> R.drawable.ic_amenity_free_hours
    RoomAmenity.GOVERNMENT_UTILITY_RATES -> R.drawable.ic_amenity_bolt
    RoomAmenity.SECURED_PARKING -> R.drawable.ic_amenity_parking
    RoomAmenity.COOKING -> R.drawable.ic_amenity_cooking
    RoomAmenity.HIGH_SPEED_WIFI -> R.drawable.ic_amenity_wifi
    RoomAmenity.HOT_WATER -> R.drawable.ic_amenity_hot_water
    RoomAmenity.BALCONY -> R.drawable.ic_amenity_balcony
    RoomAmenity.KEY_CARD -> R.drawable.ic_amenity_key_card
}

private const val METERS_PER_KM = 1000

@Composable
private fun distanceText(meters: Int): String = if (meters < METERS_PER_KM) {
    stringResource(R.string.housing_distance_m, meters)
} else {
    stringResource(R.string.housing_distance_km, meters / METERS_PER_KM.toFloat())
}

/** Chữ cái đầu của từ đầu và từ cuối tên chủ tin (vd. "Trần Minh Thư" → "TT"). */
private fun initialsOf(name: String): String {
    val parts = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    return when {
        parts.isEmpty() -> "?"
        parts.size == 1 -> parts.first().take(2).uppercase()
        else -> (parts.first().take(1) + parts.last().take(1)).uppercase()
    }
}

/** Tiền luôn nhóm chữ số kiểu Việt Nam (`1.500.000đ` theo docs/Design-system.md), không theo locale máy. */
private fun formatVnd(amount: Int): String {
    val formatter = NumberFormat.getIntegerInstance(Locale.forLanguageTag("vi-VN"))
    return formatter.format(amount)
}

private val PreviewListing = HousingListing(
    id = "HS-001",
    type = HousingListingType.ROOM_FOR_RENT,
    campus = UthCampus.CS1_QUANG_TRUNG,
    title = "Phòng 20m² mặt tiền Võ Oanh, ở được ngay",
    description = "Phòng riêng trong nhà nguyên căn, cửa sổ hướng đông, gần cổng sau cơ sở 1 đi bộ 5 phút. " +
        "Khu sinh viên an ninh, yên tĩnh, hàng quán ăn uống sát bên cạnh.",
    hostName = "Trần Minh Thư",
    hostStudentId = "2251010042",
    contactPhone = "0908123456",
    contactZaloUrl = "https://zalo.me/0908123456",
    images = listOf("mock1", "mock2", "mock3"),
    monthlyRentVnd = 2_800_000,
    depositVnd = 2_000_000,
    utilitiesIncluded = false,
    availableSlots = 2,
    genderPreference = GenderPreference.ANY,
    scheduleNote = "Nhà giờ giấc tự do, có khóa thẻ ra vào ban đêm.",
    amenities = listOf(
        RoomAmenity.MACHINE_WASH,
        RoomAmenity.AIR_CONDITIONER,
        RoomAmenity.MEZZANINE,
        RoomAmenity.FREE_HOURS,
    ),
    addressLabel = "138 Võ Oanh, Bình Thạnh",
    latitude = 10.8025,
    longitude = 106.7150,
    distanceToCampusM = 320,
)

@Composable
private fun DetailStatePreview(state: HousingDetailUiState, darkTheme: Boolean = false) = UthPreview(darkTheme = darkTheme) {
    HousingDetailScreen(uiState = state, onAction = {}, onNavigateBack = {})
}

@Preview(name = "detail content light", showBackground = true)
@Composable
private fun DetailContentLightPreview() = DetailStatePreview(
    HousingDetailUiState(isLoading = false, listing = PreviewListing),
)

@Preview(name = "detail content dark", showBackground = true)
@Composable
private fun DetailContentDarkPreview() = DetailStatePreview(
    HousingDetailUiState(isLoading = false, listing = PreviewListing),
    darkTheme = true,
)
