package com.giathinh.uthconnect.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.giathinh.uthconnect.R
import com.giathinh.uthconnect.core.designsystem.component.UthCard
import com.giathinh.uthconnect.core.designsystem.theme.UthDimens
import com.giathinh.uthconnect.domain.model.BookCondition
import com.giathinh.uthconnect.domain.model.BookDealType
import com.giathinh.uthconnect.domain.model.BookListing
import com.giathinh.uthconnect.domain.model.GenderPreference
import com.giathinh.uthconnect.domain.model.HousingListing
import com.giathinh.uthconnect.domain.model.ListingKind
import com.giathinh.uthconnect.domain.model.LostFoundListing
import com.giathinh.uthconnect.domain.model.UthCampus
import java.text.NumberFormat
import java.util.Locale

@Composable
fun HomeCommunitySections(
    uiState: HomeUiState,
    onAction: (HomeAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Xl),
    ) {
        HomeHousingSection(
            listings = uiState.visibleHousingListings,
            onOpenListing = { onAction(HomeAction.OpenHousingDetail(it)) },
            onOpenAll = { onAction(HomeAction.OpenAllHousing) },
            onCreatePost = { onAction(HomeAction.CreateHousingPost) },
        )

        HomeBooksSection(
            listings = uiState.visibleBookListings,
            onOpenListing = { onAction(HomeAction.OpenBookDetail(it)) },
            onOpenAll = { onAction(HomeAction.OpenAllBooks) },
        )

        HomeLostFoundSection(
            listings = uiState.visibleLostFoundListings,
            onOpenListing = { onAction(HomeAction.OpenLostFoundDetail(it)) },
            onOpenAll = { onAction(HomeAction.OpenAllLostFound) },
            onCreatePost = { onAction(HomeAction.CreateLostFoundPost) },
        )
    }
}

@Composable
private fun HomeHousingSection(
    listings: List<HousingListing>,
    onOpenListing: (String) -> Unit,
    onOpenAll: () -> Unit,
    onCreatePost: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        HomeSectionHeader(
            icon = Icons.Default.Home,
            title = stringResource(R.string.home_community_housing_title),
            subtitle = stringResource(R.string.home_community_housing_subtitle),
            onSeeAll = onOpenAll,
            actionLabel = stringResource(R.string.home_community_post_housing),
            onAction = onCreatePost,
        )

        if (listings.isEmpty()) {
            Text(
                text = stringResource(R.string.home_community_empty_housing),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = UthDimens.Spacing.Xl, vertical = UthDimens.Spacing.Sm),
            )
        } else {
            LazyRow(
                contentPadding = PaddingValues(horizontal = UthDimens.Spacing.Xl),
                horizontalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Md),
            ) {
                items(listings, key = { it.id }) { listing ->
                    HomeHousingCard(listing = listing, onClick = { onOpenListing(listing.id) })
                }
            }
        }
    }
}

@Composable
private fun HomeHousingCard(
    listing: HousingListing,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val campusTag = when (listing.campus) {
        UthCampus.CS1_QUANG_TRUNG -> "CS1"
        UthCampus.CS2_THU_DUC -> "CS2"
        UthCampus.CS3_TO_KY -> "CS3"
    }
    val genderTag = when (listing.genderPreference) {
        GenderPreference.MALE -> "Nam"
        GenderPreference.FEMALE -> "Nữ"
        GenderPreference.ANY -> "Mọi giới tính"
    }

    UthCard(
        modifier = modifier.width(220.dp),
        onClick = onClick,
        contentPadding = false,
    ) {
        Column(modifier = Modifier.padding(UthDimens.Spacing.Md)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Text(
                        text = campusTag,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                }
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                ) {
                    Text(
                        text = genderTag,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                }
            }
            Spacer(Modifier.height(UthDimens.Spacing.Sm))
            Text(
                text = formatHousingPrice(listing.monthlyRentVnd),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = listing.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = listing.addressLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun HomeBooksSection(
    listings: List<BookListing>,
    onOpenListing: (String) -> Unit,
    onOpenAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        HomeSectionHeader(
            icon = Icons.Default.ShoppingCart,
            title = stringResource(R.string.home_community_books_title),
            subtitle = stringResource(R.string.home_community_books_subtitle),
            onSeeAll = onOpenAll,
            actionLabel = stringResource(R.string.home_community_post_book),
            onAction = onOpenAll,
        )

        if (listings.isEmpty()) {
            Text(
                text = stringResource(R.string.home_community_empty_books),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = UthDimens.Spacing.Xl, vertical = UthDimens.Spacing.Sm),
            )
        } else {
            LazyRow(
                contentPadding = PaddingValues(horizontal = UthDimens.Spacing.Xl),
                horizontalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Md),
            ) {
                items(listings, key = { it.id }) { listing ->
                    HomeBookCard(listing = listing, onClick = { onOpenListing(listing.id) })
                }
            }
        }
    }
}

@Composable
private fun HomeBookCard(
    listing: BookListing,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val conditionTag = when (listing.condition) {
        BookCondition.NEW -> "Mới"
        BookCondition.GOOD -> "Tốt"
        BookCondition.USED -> "Đã dùng"
    }

    UthCard(
        modifier = modifier.width(170.dp),
        onClick = onClick,
        contentPadding = false,
    ) {
        Column(modifier = Modifier.padding(UthDimens.Spacing.Md)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .clip(UthDimens.Radius.MdShape)
                    .background(MaterialTheme.colorScheme.tertiaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "📚 ${listing.subject.take(15)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(UthDimens.Spacing.Sm))
            Text(
                text = if (listing.dealType == BookDealType.EXCHANGE) {
                    "Trao đổi"
                } else {
                    "${NumberFormat.getNumberInstance(Locale.GERMANY).format(listing.priceVnd ?: 0)}đ"
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.tertiary,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = listing.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = listing.sellerName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = conditionTag,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun HomeLostFoundSection(
    listings: List<LostFoundListing>,
    onOpenListing: (String) -> Unit,
    onOpenAll: () -> Unit,
    onCreatePost: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        HomeSectionHeader(
            icon = Icons.Default.Search,
            title = stringResource(R.string.home_community_lostfound_title),
            subtitle = stringResource(R.string.home_community_lostfound_subtitle),
            onSeeAll = onOpenAll,
            actionLabel = stringResource(R.string.home_community_post_lostfound),
            onAction = onCreatePost,
        )

        if (listings.isEmpty()) {
            Text(
                text = stringResource(R.string.home_community_empty_lostfound),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = UthDimens.Spacing.Xl, vertical = UthDimens.Spacing.Sm),
            )
        } else {
            LazyRow(
                contentPadding = PaddingValues(horizontal = UthDimens.Spacing.Xl),
                horizontalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Md),
            ) {
                items(listings, key = { it.id }) { listing ->
                    HomeLostFoundCard(listing = listing, onClick = { onOpenListing(listing.id) })
                }
            }
        }
    }
}

@Composable
private fun HomeLostFoundCard(
    listing: LostFoundListing,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isLost = listing.kind == ListingKind.LOST
    val kindLabel = if (isLost) "MẤT ĐỒ" else "NHẶT ĐƯỢC"
    val kindContainerColor = if (isLost) {
        MaterialTheme.colorScheme.errorContainer
    } else {
        MaterialTheme.colorScheme.primaryContainer
    }
    val kindTextColor = if (isLost) {
        MaterialTheme.colorScheme.onErrorContainer
    } else {
        MaterialTheme.colorScheme.onPrimaryContainer
    }

    UthCard(
        modifier = modifier.width(200.dp),
        onClick = onClick,
        contentPadding = false,
    ) {
        Column(modifier = Modifier.padding(UthDimens.Spacing.Md)) {
            Surface(
                shape = CircleShape,
                color = kindContainerColor,
            ) {
                Text(
                    text = kindLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = kindTextColor,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                )
            }
            Spacer(Modifier.height(UthDimens.Spacing.Sm))
            Text(
                text = listing.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp),
                )
                Spacer(Modifier.width(2.dp))
                Text(
                    text = listing.location,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun HomeSectionHeader(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onSeeAll: () -> Unit,
    actionLabel: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = UthDimens.Spacing.Xl, vertical = UthDimens.Spacing.Sm),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f),
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                OutlinedButton(
                    onClick = onAction,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary,
                    ),
                    modifier = Modifier.height(32.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(Modifier.width(2.dp))
                    Text(
                        text = actionLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
                TextButton(
                    onClick = onSeeAll,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(32.dp),
                ) {
                    Text(
                        text = stringResource(R.string.home_community_see_all),
                        style = MaterialTheme.typography.labelSmall,
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                    )
                }
            }
        }
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private const val ONE_MILLION_DOUBLE = 1_000_000.0
private const val ONE_MILLION_LONG = 1_000_000L

private fun formatHousingPrice(priceVnd: Int): String {
    val million = priceVnd / ONE_MILLION_DOUBLE
    return if (million >= 1.0) {
        if (priceVnd % ONE_MILLION_LONG.toInt() == 0) {
            "${million.toLong()} Tr/tháng"
        } else {
            String.format(Locale.US, "%.1f Tr/tháng", million)
        }
    } else {
        "${NumberFormat.getNumberInstance(Locale.GERMANY).format(priceVnd)}đ/tháng"
    }
}
