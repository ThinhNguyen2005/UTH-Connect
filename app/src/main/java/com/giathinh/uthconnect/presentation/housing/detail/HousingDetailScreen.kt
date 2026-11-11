package com.giathinh.uthconnect.presentation.housing.detail

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
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
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
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
import com.giathinh.uthconnect.domain.model.HousingContactRequest
import com.giathinh.uthconnect.domain.model.HousingContactStatus
import com.giathinh.uthconnect.domain.model.HousingListing
import com.giathinh.uthconnect.domain.model.HousingListingStatus
import com.giathinh.uthconnect.domain.model.HousingListingType
import com.giathinh.uthconnect.domain.model.HousingPrivateDetails
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
                DetailBottomBar(uiState = uiState, onAction = onAction)
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
                    uiState = uiState,
                    onAction = onAction,
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
    uiState: HousingDetailUiState,
    onAction: (HousingDetailAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listing = uiState.listing ?: return
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

        if (uiState.isOwner && uiState.contactRequests.isNotEmpty()) {
            item {
                ContactRequestsCard(
                    requests = uiState.contactRequests,
                    isActionLoading = uiState.isActionLoading,
                    onRespond = { requestId, accept ->
                        onAction(HousingDetailAction.RespondContact(requestId, accept))
                    },
                )
            }
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
            MapCard(
                listing = listing,
                privateDetails = uiState.privateDetails,
                onOpenMap = { onAction(HousingDetailAction.OpenMap) },
            )
        }
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
                .aspectRatio(4f / 3f)
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

/** Ô nhấn khoảng cách tới cơ sở — dữ liệu thật từ `distanceToCampusM` (tọa độ xấp xỉ). */
@Composable
private fun ProximityBox(listing: HousingListing, modifier: Modifier = Modifier) {
    val proximityText = if (listing.hasLocation) {
        stringResource(
            R.string.housing_detail_proximity,
            stringResource(listing.campus.labelRes()),
            distanceText(listing.distanceToCampusM),
        )
    } else {
        stringResource(R.string.housing_detail_no_location)
    }
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
                Text(
                    text = proximityText,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = stringResource(R.string.housing_detail_proximity_sub),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
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
                Text(
                    text = stringResource(amenity.subtitleRes()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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

/** Khung vị trí: trước mở khóa chỉ hiện khu vực xấp xỉ, sau ACCEPTED hiện địa chỉ đầy đủ. */
@Composable
private fun MapCard(
    listing: HousingListing,
    privateDetails: HousingPrivateDetails?,
    onOpenMap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val mapPreviewDescription = stringResource(R.string.housing_detail_map_preview)
    val campusLabel = stringResource(listing.campus.labelRes())
    val locationLabel = privateDetails?.exactAddress?.takeIf { it.isNotBlank() }
        ?: listing.addressLabel.ifBlank { campusLabel }
    val canOpenMap = listing.hasLocation || privateDetails != null
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
                    text = locationLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = UthTheme.extendedColors.textSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                )
                if (privateDetails == null) {
                    Text(
                        text = stringResource(R.string.housing_detail_reveal_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            if (canOpenMap) {
                val directionsLabel = if (privateDetails != null) {
                    stringResource(R.string.housing_detail_directions_exact)
                } else {
                    stringResource(R.string.housing_detail_directions_approx, campusLabel)
                }
                UthPrimaryButton(
                    text = directionsLabel,
                    onClick = onOpenMap,
                    leadingIcon = Icons.Filled.Place,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(UthDimens.Spacing.Sm),
                )
            }
        }
    }
}

/** Thanh hành động cố định theo trạng thái mở khóa: chủ tin / đã ACCEPTED / đang chờ / chưa hỏi. */
@Composable
private fun DetailBottomBar(
    uiState: HousingDetailUiState,
    onAction: (HousingDetailAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listing = uiState.listing ?: return
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
            when {
                uiState.isOwner -> {
                    UthPrimaryButton(
                        text = stringResource(R.string.housing_detail_mark_fulfilled),
                        onClick = { onAction(HousingDetailAction.MarkFulfilled) },
                        enabled = listing.status == HousingListingStatus.OPEN,
                        loading = uiState.isActionLoading,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                uiState.isRevealed -> {
                    ContactPills(
                        onCall = { onAction(HousingDetailAction.CallHost) },
                        onZalo = { onAction(HousingDetailAction.OpenZalo) },
                    )
                }

                uiState.myRequest?.status == HousingContactStatus.PENDING -> {
                    UthStatusBadge(
                        text = stringResource(R.string.housing_detail_request_pending),
                        tone = BadgeTone.Warning,
                    )
                    TextButton(
                        onClick = { onAction(HousingDetailAction.CancelContact) },
                        enabled = !uiState.isActionLoading,
                    ) {
                        Text(text = stringResource(R.string.housing_detail_cancel_request))
                    }
                }

                else -> {
                    if (uiState.myRequest?.status == HousingContactStatus.REJECTED) {
                        Text(
                            text = stringResource(R.string.housing_detail_request_rejected),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                    UthPrimaryButton(
                        text = stringResource(R.string.housing_detail_request_contact),
                        onClick = { onAction(HousingDetailAction.RequestContact) },
                        enabled = listing.status == HousingListingStatus.OPEN,
                        loading = uiState.isActionLoading,
                        modifier = Modifier.fillMaxWidth(),
                    )
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

/** Cặp nút Gọi điện + Nhắn Zalo, chỉ hiện sau khi chủ tin chấp nhận yêu cầu liên hệ. */
@Composable
private fun ContactPills(
    onCall: () -> Unit,
    onZalo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            onClick = onCall,
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

/** Phía chủ tin: danh sách yêu cầu liên hệ, chấp nhận/từ chối ngay trên tin. */
@Composable
private fun ContactRequestsCard(
    requests: List<HousingContactRequest>,
    isActionLoading: Boolean,
    onRespond: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    UthCard(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.housing_detail_requests_heading),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(UthDimens.Spacing.Md))
        Column(verticalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Sm)) {
            requests.forEach { request ->
                ContactRequestRow(
                    request = request,
                    isActionLoading = isActionLoading,
                    onRespond = onRespond,
                )
            }
        }
    }
}

@Composable
private fun ContactRequestRow(
    request: HousingContactRequest,
    isActionLoading: Boolean,
    onRespond: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val unknownRequester = stringResource(R.string.housing_detail_requester_unknown)
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = UthTheme.extendedColors.surfaceSubtle,
    ) {
        Row(
            modifier = Modifier.padding(UthDimens.Spacing.Md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Sm),
        ) {
            Icon(
                imageVector = Icons.Filled.Person,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(UthDimens.Size.Icon),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = request.requesterName.ifBlank { unknownRequester },
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (request.message.isNotBlank()) {
                    Text(
                        text = request.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            when (request.status) {
                HousingContactStatus.PENDING -> {
                    TextButton(
                        onClick = { onRespond(request.id, false) },
                        enabled = !isActionLoading,
                    ) {
                        Text(text = stringResource(R.string.housing_detail_reject))
                    }
                    TextButton(
                        onClick = { onRespond(request.id, true) },
                        enabled = !isActionLoading,
                    ) {
                        Text(text = stringResource(R.string.housing_detail_accept))
                    }
                }

                HousingContactStatus.ACCEPTED -> {
                    UthStatusBadge(
                        text = stringResource(R.string.housing_detail_request_accepted),
                        tone = BadgeTone.Success,
                    )
                }

                HousingContactStatus.REJECTED -> {
                    UthStatusBadge(
                        text = stringResource(R.string.housing_detail_status_rejected),
                        tone = BadgeTone.Danger,
                    )
                }

                HousingContactStatus.CANCELLED -> {
                    UthStatusBadge(
                        text = stringResource(R.string.housing_detail_request_cancelled),
                        tone = BadgeTone.Neutral,
                    )
                }
            }
        }
    }
}

/** Skeleton trạng thái tải. */
@Composable
private fun HousingDetailSkeleton(modifier: Modifier = Modifier) {
    val loadingLabel = stringResource(R.string.housing_loading)
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(UthDimens.Spacing.Lg)
            .semantics { contentDescription = loadingLabel },
        verticalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Md),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(4f / 3f)
                .clip(MaterialTheme.shapes.medium)
                .background(UthTheme.extendedColors.surfaceSubtle),
        )
        HousingSkeletonCard()
        HousingSkeletonCard()
    }
}

@Composable
private fun HousingSkeletonCard(modifier: Modifier = Modifier) {
    UthCard(modifier = modifier.fillMaxWidth()) {
        SkeletonLine(
            fraction = UthDimens.Layout.HousingSkeletonTitleFraction,
            height = UthDimens.Size.Icon,
        )
        Spacer(modifier = Modifier.height(UthDimens.Spacing.Sm))
        SkeletonLine(
            fraction = UthDimens.Layout.HousingSkeletonBodyFraction,
            height = UthDimens.Size.IconInline,
        )
        SkeletonLine(
            fraction = UthDimens.Layout.HousingSkeletonBodyFraction,
            height = UthDimens.Size.IconInline,
        )
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

@StringRes
private fun RoomAmenity.subtitleRes(): Int = when (this) {
    RoomAmenity.MEZZANINE -> R.string.housing_amenity_mezzanine_sub
    RoomAmenity.AIR_CONDITIONER -> R.string.housing_amenity_ac_sub
    RoomAmenity.MACHINE_WASH -> R.string.housing_amenity_washing_sub
    RoomAmenity.FREE_HOURS -> R.string.housing_amenity_free_hours_sub
    RoomAmenity.GOVERNMENT_UTILITY_RATES -> R.string.housing_amenity_bolt_sub
    RoomAmenity.SECURED_PARKING -> R.string.housing_amenity_parking_sub
    RoomAmenity.COOKING -> R.string.housing_amenity_cooking_sub
    RoomAmenity.HIGH_SPEED_WIFI -> R.string.housing_amenity_wifi_sub
    RoomAmenity.HOT_WATER -> R.string.housing_amenity_hot_water_sub
    RoomAmenity.BALCONY -> R.string.housing_amenity_balcony_sub
    RoomAmenity.KEY_CARD -> R.string.housing_amenity_key_card_sub
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
    title = "Phòng 20m² mặt tiền Quang Trung, ở được ngay",
    description = "Phòng riêng trong nhà nguyên căn, cửa sổ hướng đông, gần cổng sau cơ sở 1 đi bộ 5 phút. " +
        "Khu sinh viên an ninh, yên tĩnh, hàng quán ăn uống sát bên cạnh.",
    hostName = "Trần Minh Thư",
    hostStudentId = "2251010042",
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
    addressLabel = "Khu vực Quang Trung, Gò Vấp",
    approxLatitude = 10.83,
    approxLongitude = 106.66,
    distanceToCampusM = 320,
)

private val PreviewPrivateDetails = HousingPrivateDetails(
    latitude = 10.8313,
    longitude = 106.6553,
    exactAddress = "138 Quang Trung, Phường 10, Gò Vấp",
    contactPhone = "0908123456",
    contactZaloUrl = "https://zalo.me/0908123456",
)

private val PreviewRequest = HousingContactRequest(
    id = "RQ-001",
    listingId = "HS-001",
    requesterId = "SV-002",
    requesterName = "Nguyễn Văn An",
    status = HousingContactStatus.PENDING,
    message = "Mình muốn xem phòng cuối tuần này.",
    createdAtMillis = 0L,
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

@Preview(name = "detail request pending", showBackground = true)
@Composable
private fun DetailRequestPendingPreview() = DetailStatePreview(
    HousingDetailUiState(isLoading = false, listing = PreviewListing, myRequest = PreviewRequest),
)

@Preview(name = "detail revealed", showBackground = true)
@Composable
private fun DetailRevealedPreview() = DetailStatePreview(
    HousingDetailUiState(
        isLoading = false,
        listing = PreviewListing,
        myRequest = PreviewRequest.copy(status = HousingContactStatus.ACCEPTED),
        privateDetails = PreviewPrivateDetails,
    ),
)

@Preview(name = "detail owner requests", showBackground = true)
@Composable
private fun DetailOwnerPreview() = DetailStatePreview(
    HousingDetailUiState(
        isLoading = false,
        listing = PreviewListing.copy(isOwner = true),
        contactRequests = listOf(PreviewRequest),
        privateDetails = PreviewPrivateDetails,
    ),
)
