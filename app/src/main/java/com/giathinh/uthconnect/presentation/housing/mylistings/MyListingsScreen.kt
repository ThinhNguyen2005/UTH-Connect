package com.giathinh.uthconnect.presentation.housing.mylistings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.giathinh.uthconnect.R
import com.giathinh.uthconnect.core.designsystem.component.BadgeTone
import com.giathinh.uthconnect.core.designsystem.component.UthCard
import com.giathinh.uthconnect.core.designsystem.component.UthEmptyState
import com.giathinh.uthconnect.core.designsystem.component.UthStatusBadge
import com.giathinh.uthconnect.core.designsystem.theme.UthDimens
import com.giathinh.uthconnect.core.designsystem.theme.UthPreview
import com.giathinh.uthconnect.core.designsystem.theme.UthTheme
import com.giathinh.uthconnect.domain.model.GenderPreference
import com.giathinh.uthconnect.domain.model.HousingListing
import com.giathinh.uthconnect.domain.model.HousingListingStatus
import com.giathinh.uthconnect.domain.model.HousingListingType
import com.giathinh.uthconnect.domain.model.UthCampus
import com.giathinh.uthconnect.presentation.housing.labelRes
import java.text.NumberFormat
import java.util.Locale

/**
 * "Tin của tôi" — stateless, nhận [MyListingsUiState] và phát [MyListingsAction]
 * (docs/CODING_STANDARDS.md mục 3.2). Liệt kê tin của chính người đăng ở MỌI trạng
 * thái (kể cả PENDING/REJECTED bị ẩn khỏi feed) để chủ tin theo dõi kiểm duyệt.
 * Không có nút xóa ở đây — màn chi tiết đã có xóa và đánh dấu hết phòng.
 */
@Composable
fun MyListingsScreen(
    uiState: MyListingsUiState,
    onAction: (MyListingsAction) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { MyListingsHeader(onNavigateBack = onNavigateBack) },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when {
                uiState.isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))

                uiState.failure != null -> UthEmptyState(
                    title = stringResource(R.string.housing_load_error_title),
                    body = stringResource(R.string.housing_load_error_body),
                    icon = Icons.Filled.Warning,
                    actionLabel = stringResource(R.string.common_retry),
                    onAction = { onAction(MyListingsAction.Load) },
                )

                uiState.isEmpty -> UthEmptyState(
                    title = stringResource(R.string.housing_my_listings_title),
                    body = stringResource(R.string.housing_my_listings_empty),
                    icon = Icons.Filled.Search,
                )

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(UthDimens.Spacing.Lg),
                    verticalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Md),
                ) {
                    items(items = uiState.listings, key = { it.id }) { listing ->
                        MyListingCard(
                            listing = listing,
                            onClick = { onAction(MyListingsAction.OpenListing(listing.id)) },
                        )
                    }
                }
            }
        }
    }
}

/** Header nền xanh thương hiệu UTH: nút Back + tiêu đề "Tin của tôi". */
@Composable
private fun MyListingsHeader(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primary)
            .padding(horizontal = UthDimens.Spacing.Sm, vertical = UthDimens.Spacing.Sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onNavigateBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.common_back),
                tint = MaterialTheme.colorScheme.onPrimary,
            )
        }
        Text(
            text = stringResource(R.string.housing_my_listings_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimary,
        )
    }
}

@Composable
private fun MyListingCard(
    listing: HousingListing,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    UthCard(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
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
            Spacer(modifier = Modifier.width(UthDimens.Spacing.Sm))
            StatusBadge(status = listing.status)
        }
        Spacer(modifier = Modifier.height(UthDimens.Spacing.Xs))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.LocationOn,
                contentDescription = null,
                modifier = Modifier.size(UthDimens.Size.IconInline),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.width(UthDimens.Spacing.Xs))
            Text(
                text = stringResource(listing.campus.labelRes()),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = " · ",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
            )
            Text(
                text = stringResource(listing.type.labelRes()),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(modifier = Modifier.height(UthDimens.Spacing.Sm))
        Text(
            text = stringResource(R.string.housing_price_per_month, formatVnd(listing.monthlyRentVnd)),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        if (listing.status == HousingListingStatus.REJECTED && !listing.rejectionReason.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(UthDimens.Spacing.Sm))
            Text(
                text = stringResource(R.string.housing_my_listings_rejection, listing.rejectionReason),
                style = MaterialTheme.typography.bodySmall,
                color = UthTheme.extendedColors.danger,
            )
        }
    }
}

@Composable
private fun StatusBadge(status: HousingListingStatus) {
    val (text, tone) = when (status) {
        HousingListingStatus.PENDING -> stringResource(R.string.housing_status_pending) to BadgeTone.Warning
        HousingListingStatus.OPEN -> stringResource(R.string.housing_status_open) to BadgeTone.Success
        HousingListingStatus.REJECTED -> stringResource(R.string.housing_status_rejected) to BadgeTone.Danger
        HousingListingStatus.FULFILLED -> stringResource(R.string.housing_status_fulfilled) to BadgeTone.Neutral
        HousingListingStatus.HIDDEN_BY_REPORTS -> stringResource(R.string.housing_status_hidden) to BadgeTone.Neutral
    }
    UthStatusBadge(text = text, tone = tone)
}

private fun formatVnd(amount: Int): String {
    val formatter = NumberFormat.getIntegerInstance(Locale.forLanguageTag("vi-VN"))
    return formatter.format(amount)
}

private val PreviewListing = HousingListing(
    type = HousingListingType.ROOM_FOR_RENT,
    campus = UthCampus.CS1_QUANG_TRUNG,
    title = "Phòng 20m² mặt tiền Võ Oanh, ở được ngay",
    description = "Phòng riêng trong nhà nguyên căn, gần cổng sau cơ sở 1.",
    hostName = "Trần Minh Thư",
    monthlyRentVnd = 2_800_000,
    depositVnd = 2_000_000,
    utilitiesIncluded = false,
    availableSlots = 2,
    genderPreference = GenderPreference.ANY,
    scheduleNote = "Nhà giờ giấc tự do, có khóa thẻ ra vào ban đêm.",
    addressLabel = "138 Võ Oanh, Bình Thạnh",
    latitude = 10.8025,
    longitude = 106.7150,
    status = HousingListingStatus.REJECTED,
    rejectionReason = "Ảnh không rõ, vui lòng bổ sung ảnh phòng.",
)

private val PreviewLoading = MyListingsUiState(isLoading = true)
private val PreviewFailure = MyListingsUiState(isLoading = false, failure = MyListingsFailure.LoadFailed)
private val PreviewEmpty = MyListingsUiState(isLoading = false)
private val PreviewContent = MyListingsUiState(isLoading = false, listings = listOf(PreviewListing))

@Composable
private fun ListingsPreview(state: MyListingsUiState, darkTheme: Boolean = false) = UthPreview(darkTheme = darkTheme) {
    MyListingsScreen(uiState = state, onAction = {}, onNavigateBack = {})
}

@Preview(name = "my listings loading light", showBackground = true)
@Composable
private fun LoadingLightPreview() = ListingsPreview(PreviewLoading)

@Preview(name = "my listings loading dark", showBackground = true)
@Composable
private fun LoadingDarkPreview() = ListingsPreview(PreviewLoading, darkTheme = true)

@Preview(name = "my listings failure light", showBackground = true)
@Composable
private fun FailureLightPreview() = ListingsPreview(PreviewFailure)

@Preview(name = "my listings failure dark", showBackground = true)
@Composable
private fun FailureDarkPreview() = ListingsPreview(PreviewFailure, darkTheme = true)

@Preview(name = "my listings empty light", showBackground = true)
@Composable
private fun EmptyLightPreview() = ListingsPreview(PreviewEmpty)

@Preview(name = "my listings empty dark", showBackground = true)
@Composable
private fun EmptyDarkPreview() = ListingsPreview(PreviewEmpty, darkTheme = true)

@Preview(name = "my listings content light", showBackground = true)
@Composable
private fun ContentLightPreview() = ListingsPreview(PreviewContent)

@Preview(name = "my listings content dark", showBackground = true)
@Composable
private fun ContentDarkPreview() = ListingsPreview(PreviewContent, darkTheme = true)
