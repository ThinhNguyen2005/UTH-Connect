package com.giathinh.uthconnect.presentation.home

import androidx.annotation.StringRes
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
import com.giathinh.uthconnect.domain.model.Announcement
import com.giathinh.uthconnect.domain.model.AnnouncementCategory
import com.giathinh.uthconnect.domain.model.HomeBanner
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.abs

/**
 * Tab Trang chủ — cổng thông tin sinh viên: khối nhận diện nhà trường (nền `primary`),
 * carousel "Tiêu điểm chỉ đạo" và bảng tin lọc theo nhóm.
 *
 * Screen **stateless**: chỉ render [HomeUiState] và phát [HomeAction]. Không `hiltViewModel()`,
 * không repository — ràng buộc tầng `presentation` ở `docs/CODING_STANDARDS.md` mục 3.1.
 *
 * Khối header dùng `colorScheme.primary` chứ không một màu Navy cứng: ở dark mode token này
 * tự chuyển sang Navy sáng hoá `#7FB2E5` kèm `onPrimary` tối (`docs/Design-system.md` mục 1.2).
 */
@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onAction: (HomeAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        item(key = HomeKey.Header) {
            HomeHeader(
                unreadCount = uiState.unreadCount,
                onOpenNotifications = { onAction(HomeAction.OpenNotifications) },
            )
        }

        when {
            uiState.isLoading -> item(key = HomeKey.Loading) { HomeLoading() }

            uiState.failure != null -> item(key = HomeKey.Error) {
                HomeError(onRetry = { onAction(HomeAction.Retry) })
            }

            else -> homeFeed(
                state = uiState,
                onOpen = { onAction(HomeAction.OpenAnnouncement(it)) },
                onOpenFilter = { onAction(HomeAction.OpenFilter) },
                onOpenArchive = { onAction(HomeAction.OpenArchive) },
                onSelectCategory = { onAction(HomeAction.SelectCategory(it)) },
            )
        }
    }
}

private fun LazyListScope.homeFeed(
    state: HomeUiState,
    onOpen: (String) -> Unit,
    onOpenFilter: () -> Unit,
    onOpenArchive: () -> Unit,
    onSelectCategory: (AnnouncementCategory?) -> Unit,
) {
    item(key = HomeKey.Highlights) {
        HomeHighlightsSection(
            banners = state.banners,
            onOpen = { onOpen(it.id) },
        )
    }

    item(key = HomeKey.FeedHeader) {
        // Header + chip vẫn hiện khi danh sách rỗng có lọc: người dùng cần lối thoát
        // khỏi bộ lọc đang chọn (`docs/Design-system.md` mục 4.9 — nói nguyên nhân + cách xử lý).
        HomeFeedHeader(
            unreadCount = state.unreadCount,
            onOpenFilter = onOpenFilter,
            modifier = Modifier.padding(
                start = UthDimens.Spacing.Xl,
                end = UthDimens.Spacing.Xl,
                top = UthDimens.Spacing.Xl,
            ),
        )
    }

    item(key = HomeKey.Categories) {
        HomeCategoryChips(
            counts = state.categoryCounts,
            total = state.announcements.size,
            selected = state.selectedCategory,
            onSelect = onSelectCategory,
            modifier = Modifier.padding(
                start = UthDimens.Spacing.Xl,
                end = UthDimens.Spacing.Xl,
                top = UthDimens.Spacing.Sm,
                bottom = UthDimens.Spacing.Md,
            ),
        )
    }

    if (state.visibleAnnouncements.isEmpty()) {
        item(key = HomeKey.Empty) {
            HomeEmpty(
                modifier = Modifier.padding(horizontal = UthDimens.Spacing.Xl),
            )
        }
    } else {
        item(key = HomeKey.FeedList) {
            AnnouncementList(
                announcements = state.visibleAnnouncements,
                onOpen = { onOpen(it.id) },
                modifier = Modifier.padding(horizontal = UthDimens.Spacing.Xl),
            )
        }
    }

    item(key = HomeKey.Footer) {
        HomeFooter(
            total = state.visibleAnnouncements.size,
            onOpenArchive = onOpenArchive,
            modifier = Modifier.padding(
                start = UthDimens.Spacing.Xl,
                end = UthDimens.Spacing.Xl,
                top = UthDimens.Spacing.Md,
                bottom = UthDimens.Spacing.Xl,
            ),
        )
    }
}

/**
 * Khối nhận diện + lời chào trên nền `primary`, viền dưới không kẻ vì nền đậm tự tách
 * khỏi nội dung. Hai vòng tròn trang trí vẽ bằng [drawBehind] với alpha rất thấp — chỉ để khối
 * không phẳng, không mang thông tin nên không ảnh hưởng TalkBack.
 */
@Composable
private fun HomeHeader(
    unreadCount: Int,
    onOpenNotifications: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.primary)
            .drawBehind {
                val ringRadius = size.width * HeaderRingRadiusFraction
                val ringStroke = size.width * HeaderRingStrokeFraction
                drawCircle(
                    color = colors.onPrimary.copy(alpha = HeaderRingAlpha),
                    radius = ringRadius,
                    center = Offset(size.width + ringRadius * 0.3f, size.height),
                    style = Stroke(width = ringStroke),
                )
                drawCircle(
                    color = colors.secondary.copy(alpha = HeaderRingGoldAlpha),
                    radius = ringRadius * 0.66f,
                    center = Offset(size.width * 0.92f, 0f),
                    style = Stroke(width = ringStroke * 0.6f),
                )
            }
            .padding(
                start = UthDimens.Spacing.Xl,
                end = UthDimens.Spacing.Xl,
                top = UthDimens.Spacing.Lg,
                bottom = UthDimens.Spacing.Xl,
            ),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            HomeBrandMark()
            Spacer(Modifier.width(UthDimens.Spacing.Md))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.home_uni_name).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(R.string.home_uni_tagline),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onPrimary.copy(alpha = HeaderMetaAlpha),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(UthDimens.Spacing.Sm))
            HomeNotificationButton(unreadCount = unreadCount, onClick = onOpenNotifications)
        }

        Spacer(Modifier.height(UthDimens.Spacing.Xl))

        Text(
            text = stringResource(R.string.home_greeting_hello),
            style = MaterialTheme.typography.bodySmall,
            color = colors.onPrimary.copy(alpha = HeaderMetaAlpha),
        )
        Text(
            text = stringResource(R.string.home_greeting_name),
            style = MaterialTheme.typography.titleLarge,
            color = colors.onPrimary,
        )
        Text(
            text = stringResource(R.string.home_greeting_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = colors.onPrimary.copy(alpha = HeaderMetaAlpha),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Ô vuông trắng mang chữ tắt trường — dấu nhận diện, không phải nút bấm được. */
@Composable
private fun HomeBrandMark(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(UthDimens.Size.Avatar)
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.onPrimary),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.home_logo_abbr),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

/**
 * Chuông thông báo: vùng chạm [UthDimens.Size.MinTouchTarget], hình nhìn thấy
 * [UthDimens.Size.Avatar]. Chấm Gold chỉ là nền (`docs/Design-system.md` mục 1.4) và luôn
 * đi kèm số chưa đọc trong `contentDescription` — trạng thái không truyền tải chỉ bằng màu.
 */
@Composable
private fun HomeNotificationButton(
    unreadCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val label = if (unreadCount > 0) {
        stringResource(R.string.home_notifications_unread, unreadCount)
    } else {
        stringResource(R.string.home_notifications)
    }

    Box(
        modifier = modifier
            .size(UthDimens.Size.MinTouchTarget)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(UthDimens.Size.Avatar)
                .clip(MaterialTheme.shapes.small)
                .background(colors.onPrimary.copy(alpha = HeaderButtonFillAlpha))
                .border(
                    width = UthDimens.Size.Stroke,
                    color = colors.onPrimary.copy(alpha = HeaderButtonBorderAlpha),
                    shape = MaterialTheme.shapes.small,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Notifications,
                contentDescription = null,
                tint = colors.onPrimary,
                modifier = Modifier.size(UthDimens.Size.Icon),
            )
            if (unreadCount > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(UthDimens.Spacing.Sm)
                        .size(UthDimens.Size.IndicatorDot)
                        .clip(CircleShape)
                        .border(
                            width = UthDimens.Size.StrokeFocused,
                            color = colors.primary,
                            shape = CircleShape,
                        )
                        .background(colors.secondary),
                )
            }
        }
    }
}

/**
 * Section "Tiêu điểm chỉ đạo": tiêu đề + bộ đếm trang, carousel snap ngang và chấm trang.
 *
 * Cả ba phần **dùng chung một** `LazyListState` nên bộ đếm luôn khớp thẻ đang xem — tách
 * chúng thành hai item của `LazyColumn` sẽ không chia sẻ được state (state cuộn là state UI,
 * không thuộc [HomeUiState]).
 */
@Composable
private fun HomeHighlightsSection(
    banners: List<HomeBanner>,
    onOpen: (HomeBanner) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (banners.isEmpty()) return

    val listState = rememberLazyListState()

    // Thẻ đang xem là thẻ gần **giữa khung nhìn**, không phải `firstVisibleItemIndex`: carousel
    // snap giữa và mỗi thẻ chỉ rộng [BannerWidthFraction], nên sau khi lướt sang thẻ kế tiếp thẻ
    // cũ vẫn còn "ló" ở mép trái — `firstVisibleItemIndex` không tăng và bộ đếm đứng yên ở 1.
    val position by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val center = (info.viewportStartOffset + info.viewportEndOffset) / 2
            info.visibleItemsInfo
                .minByOrNull { item -> abs(item.offset + item.size / 2 - center) }
                ?.index ?: 0
        }
    }
    val page = (position + 1).coerceAtMost(banners.size)

    Column(modifier = modifier.fillMaxWidth()) {
        HomeHighlightHeader(
            position = page,
            total = banners.size,
            modifier = Modifier.padding(
                start = UthDimens.Spacing.Xl,
                end = UthDimens.Spacing.Xl,
                top = UthDimens.Spacing.Xl,
                bottom = UthDimens.Spacing.Md,
            ),
        )
        HomeBannerCarousel(
            banners = banners,
            listState = listState,
            selectedIndex = position,
            onOpen = onOpen,
        )
    }
}

/** Tên section "Tiêu điểm chỉ đạo" + bộ đếm trang của carousel ngay bên phải. */
@Composable
private fun HomeHighlightHeader(
    position: Int,
    total: Int,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(UthDimens.Spacing.Sm)
                .height(UthDimens.Size.IconBadge)
                .clip(MaterialTheme.shapes.extraSmall)
                .background(colors.primary),
        )
        Spacer(Modifier.width(UthDimens.Spacing.Sm))
        Text(
            text = stringResource(R.string.home_highlights).uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = colors.primary,
            modifier = Modifier.weight(1f),
        )
        if (total > 0) {
            Text(
                text = stringResource(R.string.home_banner_counter, position, total),
                style = MaterialTheme.typography.labelSmall,
                color = UthTheme.extendedColors.textSecondary,
            )
        }
    }
}

/**
 * Carousel snap ngang: mỗi thẻ chiếm [BannerWidthFraction] bề rộng để thẻ kế tiếp "ló" ra,
 * gợi ý còn nội dung. `key` theo id để state cuộn không nhảy khi dữ liệu đổi.
 */
@Composable
private fun HomeBannerCarousel(
    banners: List<HomeBanner>,
    listState: LazyListState,
    selectedIndex: Int,
    onOpen: (HomeBanner) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        state = listState,
        contentPadding = PaddingValues(horizontal = UthDimens.Spacing.Xl),
        horizontalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Md),
        flingBehavior = rememberSnapFlingBehavior(lazyListState = listState),
    ) {
        items(items = banners, key = { it.id }) { banner ->
            HomeBannerCard(
                banner = banner,
                onClick = { onOpen(banner) },
                // Thẻ chiếm phần lớn bề rộng để thẻ kế tiếp "ló" ra,
                // gợi ý còn nội dung.
                modifier = Modifier.fillParentMaxWidth(BannerWidthFraction),
            )
        }
    }
    HomePageIndicator(
        count = banners.size,
        selectedIndex = selectedIndex,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = UthDimens.Spacing.Md, bottom = UthDimens.Spacing.Sm),
    )
}

/** Chấm trang: mục đang chọn là thanh dài `IndicatorBar`, các mục còn lại là chấm tròn. */
@Composable
private fun HomePageIndicator(
    count: Int,
    selectedIndex: Int,
    modifier: Modifier = Modifier,
) {
    if (count <= 1) return

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(count) { index ->
            val isSelected = index == selectedIndex
            if (index > 0) Spacer(Modifier.width(UthDimens.Spacing.Sm))
            Box(
                modifier = Modifier
                    .width(if (isSelected) UthDimens.Size.IndicatorBar else UthDimens.Size.IndicatorDot)
                    .height(UthDimens.Size.IndicatorDot)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outline
                        },
                    ),
            )
        }
    }
}

/** Thẻ banner: ảnh bìa 16:9 (mục 3.3) + badge nhóm + nút mở tròn + hàng văn bản. */
@Composable
private fun HomeBannerCard(
    banner: HomeBanner,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val categoryLabel = stringResource(banner.announcement.category.labelRes())

    UthCard(
        modifier = modifier,
        onClick = onClick,
        contentPadding = false,
    ) {
        HomeBannerCover(
            categoryLabel = categoryLabel,
            category = banner.announcement.category,
            documentCode = banner.documentCode,
            publishedAt = banner.announcement.publishedAt,
        )
        Column(modifier = Modifier.padding(UthDimens.Spacing.Lg)) {
            Text(
                text = banner.announcement.title,
                style = MaterialTheme.typography.titleMedium,
                color = colors.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(UthDimens.Spacing.Md))
            HorizontalDivider(
                thickness = UthDimens.Size.Stroke,
                color = colors.outlineVariant,
            )
            Spacer(Modifier.height(UthDimens.Spacing.Md))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = banner.announcement.publisher,
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = banner.announcement.source
                            ?: FeedDateFormatter.format(banner.announcement.publishedAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = UthTheme.extendedColors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.width(UthDimens.Spacing.Sm))
                HomeBannerAction(isMandatory = banner.isMandatory)
            }
        }
    }
}

/**
 * Ảnh bìa: chưa có ảnh thật nên dùng gradient `PrimaryContainer → SurfaceSubtle` + icon nhóm
 * chìm, đúng tinh thần "mặc định `SurfaceSubtle` khi chưa tải xong" (mục 3.3). Mọi chữ trên
 * ảnh nằm trong chip nền `surface` (alpha cao) để tương phản không phụ thuộc ảnh.
 */
@Composable
private fun HomeBannerCover(
    categoryLabel: String,
    category: AnnouncementCategory,
    documentCode: String?,
    publishedAt: LocalDateTime,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(CoverAspectRatio)
            .background(
                Brush.linearGradient(
                    listOf(colors.primaryContainer, UthTheme.extendedColors.surfaceSubtle),
                ),
            ),
    ) {
        Icon(
            imageVector = category.icon(),
            contentDescription = null,
            tint = colors.primary.copy(alpha = CoverGlyphAlpha),
            modifier = Modifier
                .align(Alignment.Center)
                .size(UthDimens.Size.AvatarLarge),
        )

        UthStatusBadge(
            text = categoryLabel.uppercase(),
            tone = BadgeTone.Accent,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(UthDimens.Spacing.Md),
        )

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(UthDimens.Spacing.Md)
                .size(UthDimens.Size.ChipHeight)
                .clip(CircleShape)
                .background(colors.surface.copy(alpha = CoverChipAlpha)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = stringResource(R.string.home_open_banner),
                tint = colors.primary,
                modifier = Modifier.size(UthDimens.Size.IconInline),
            )
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(UthDimens.Spacing.Md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (documentCode != null) {
                Text(
                    text = stringResource(R.string.home_banner_document_code, documentCode),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .clip(MaterialTheme.shapes.extraSmall)
                        .background(colors.surface.copy(alpha = CoverChipAlpha))
                        .padding(horizontal = UthDimens.Spacing.Sm, vertical = UthDimens.Spacing.Xxs),
                )
                Spacer(Modifier.width(UthDimens.Spacing.Sm))
            }
            Text(
                text = FeedDateFormatter.format(publishedAt),
                style = MaterialTheme.typography.labelSmall,
                color = colors.primary,
                modifier = Modifier
                    .clip(MaterialTheme.shapes.extraSmall)
                    .background(colors.surface.copy(alpha = CoverChipAlpha))
                    .padding(horizontal = UthDimens.Spacing.Sm, vertical = UthDimens.Spacing.Xxs),
            )
        }
    }
}

/**
 * Hành động của banner. Chữ "Bắt buộc" dùng `UthGoldText` (đậm, 7.2:1 trên trắng) —
 * **không** dùng Gold làm chữ vì chỉ đạt 2.0:1 (`docs/Design-system.md` mục 1.4).
 */
@Composable
private fun HomeBannerAction(
    isMandatory: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(
                if (isMandatory) R.string.home_banner_mandatory else R.string.home_banner_detail,
            ),
            style = MaterialTheme.typography.labelMedium,
            color = if (isMandatory) {
                MaterialTheme.colorScheme.onSecondaryContainer
            } else {
                MaterialTheme.colorScheme.primary
            },
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = if (isMandatory) {
                MaterialTheme.colorScheme.onSecondaryContainer
            } else {
                MaterialTheme.colorScheme.primary
            },
            modifier = Modifier.size(UthDimens.Size.IconBadge),
        )
    }
}

/** Tiêu đề bảng tin + badge số mới + nút "Bộ lọc" (chạm 48dp, hình 32dp theo mục 4.4). */
@Composable
private fun HomeFeedHeader(
    unreadCount: Int,
    onOpenFilter: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.home_feed_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.primary,
                )
                if (unreadCount > 0) {
                    Spacer(Modifier.width(UthDimens.Spacing.Sm))
                    UthStatusBadge(
                        text = stringResource(R.string.home_new_count, unreadCount),
                        tone = BadgeTone.Accent,
                        icon = null,
                    )
                }
            }
            Text(
                text = stringResource(R.string.home_feed_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = UthTheme.extendedColors.textSecondary,
            )
        }
        Spacer(Modifier.width(UthDimens.Spacing.Sm))
        HomeFilterButton(onClick = onOpenFilter)
    }
}

/**
 * Nút "Bộ lọc": icon + chữ, không chỉ icon (mục 6 — người mù màu vẫn đọc được ý nghĩa).
 * Bọc trong vùng chạm [UthDimens.Size.MinTouchTarget] vì chip chỉ cao [UthDimens.Size.ChipHeight].
 */
@Composable
private fun HomeFilterButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme

    Box(
        modifier = modifier
            .height(UthDimens.Size.MinTouchTarget)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier
                .height(UthDimens.Size.ChipHeight)
                .clip(MaterialTheme.shapes.small)
                .background(colors.surface)
                .border(
                    width = UthDimens.Size.Stroke,
                    color = colors.outline,
                    shape = MaterialTheme.shapes.small,
                )
                .padding(horizontal = UthDimens.Spacing.Sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.Menu,
                contentDescription = null,
                tint = colors.primary,
                modifier = Modifier.size(UthDimens.Size.IconBadge),
            )
            Spacer(Modifier.width(UthDimens.Spacing.Xs))
            Text(
                text = stringResource(R.string.home_filter),
                style = MaterialTheme.typography.labelMedium,
                color = colors.primary,
            )
        }
    }
}

/**
 * Dải chip lọc kèm số lượng mỗi nhóm. Chip đang chọn theo mục 4.4: nền `PrimaryContainer`,
 * chữ `primary` và **icon ✓ bắt buộc** để trạng thái không chỉ dựa vào màu.
 *
 * Bo góc `shapes.small` (8dp) theo mockup thiết kế; mục 4.4 ghi `Sm` (4dp) — lệch này do
 * yêu cầu thị giác, đã ghi lại để review đối chiếu.
 */
@Composable
private fun HomeCategoryChips(
    counts: Map<AnnouncementCategory, Int>,
    total: Int,
    selected: AnnouncementCategory?,
    onSelect: (AnnouncementCategory?) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Sm),
    ) {
        item(key = CategoryAllKey) {
            HomeCategoryChip(
                label = stringResource(R.string.home_category_all),
                count = total,
                selected = selected == null,
                onClick = { onSelect(null) },
            )
        }
        items(items = AnnouncementCategory.entries, key = { it.name }) { category ->
            HomeCategoryChip(
                label = stringResource(category.labelRes()),
                count = counts[category] ?: 0,
                selected = selected == category,
                onClick = { onSelect(category) },
            )
        }
    }
}

@Composable
private fun HomeCategoryChip(
    label: String,
    count: Int,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme

    Box(
        modifier = modifier
            .height(UthDimens.Size.MinTouchTarget)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier
                .height(UthDimens.Size.ChipHeight)
                .clip(MaterialTheme.shapes.small)
                .background(if (selected) colors.primaryContainer else colors.surface)
                .border(
                    width = UthDimens.Size.Stroke,
                    color = if (selected) colors.primary else colors.outline,
                    shape = MaterialTheme.shapes.small,
                )
                .padding(horizontal = UthDimens.Spacing.Md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (selected) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(UthDimens.Size.IconBadge),
                )
                Spacer(Modifier.width(UthDimens.Spacing.Xs))
            }
            Text(
                text = stringResource(R.string.home_chip_count, label, count),
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) colors.primary else colors.onSurface,
            )
        }
    }
}

/**
 * Bảng tin trong **một** card, phân tách bằng divider `UthDimens.Size.Stroke`
 * màu `outlineVariant` (mục 3.3). Divider thụt vào bằng mép trái của chữ
 * để mắt đọc theo một đường thẳng.
 */
@Composable
private fun AnnouncementList(
    announcements: List<Announcement>,
    onOpen: (Announcement) -> Unit,
    modifier: Modifier = Modifier,
) {
    UthCard(modifier = modifier, contentPadding = false) {
        announcements.forEachIndexed { index, announcement ->
            AnnouncementRow(announcement = announcement, onClick = { onOpen(announcement) })
            if (index != announcements.lastIndex) {
                HorizontalDivider(
                    modifier = Modifier.padding(
                        start = UthDimens.Spacing.Lg + UthDimens.Size.Thumbnail + UthDimens.Spacing.Md,
                        end = UthDimens.Spacing.Lg,
                    ),
                    thickness = UthDimens.Size.Stroke,
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
            }
        }
    }
}

/**
 * Một dòng bảng tin: ảnh nhỏ + tag nhóm + tiêu đề + tóm tắt + nguồn.
 *
 * Nhãn nhóm là **tag chữ** (nền `PrimaryContainer`, bo `extraSmall` — mục 4.3) chứ không
 * phải badge trạng thái: nhóm là phân loại, không phải trạng thái.
 */
@Composable
private fun AnnouncementRow(
    announcement: Announcement,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val secondary = UthTheme.extendedColors.textSecondary

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(UthDimens.Spacing.Lg),
    ) {
        AnnouncementThumbnail(category = announcement.category)
        Spacer(Modifier.width(UthDimens.Spacing.Md))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AnnouncementCategoryTag(
                    category = announcement.category,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (announcement.isUnread) {
                    Spacer(Modifier.width(UthDimens.Spacing.Sm))
                    UthStatusBadge(
                        text = stringResource(R.string.home_badge_new),
                        tone = BadgeTone.Accent,
                        icon = null,
                    )
                }
            }
            Spacer(Modifier.height(UthDimens.Spacing.Xs))
            Text(
                text = announcement.title,
                style = MaterialTheme.typography.titleSmall,
                color = colors.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (announcement.summary != null) {
                Text(
                    text = announcement.summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = secondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(UthDimens.Spacing.Xs))
            AnnouncementMeta(
                publisher = announcement.publisher,
                trailing = announcement.source
                    ?: FeedDateFormatter.format(announcement.publishedAt),
            )
        }
    }
}

/** Tag phân loại: chữ in hoa, nền `PrimaryContainer` — cặp màu đạt tương phản ở mục 1.4. */
@Composable
private fun AnnouncementCategoryTag(
    category: AnnouncementCategory,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme

    Text(
        text = stringResource(category.labelRes()).uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = if (category == AnnouncementCategory.YOUTH_UNION) {
            colors.onSecondaryContainer
        } else {
            colors.primary
        },
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
            .clip(MaterialTheme.shapes.extraSmall)
            .background(
                if (category == AnnouncementCategory.YOUTH_UNION) {
                    colors.secondaryContainer
                } else {
                    colors.primaryContainer
                },
            )
            .padding(horizontal = UthDimens.Spacing.Sm, vertical = UthDimens.Spacing.Xxs),
    )
}

/**
 * Ảnh thu nhỏ cỡ `UthDimens.Size.Thumbnail`; chưa có ảnh thật nên dùng nền tint
 * và icon nhóm (mục 3.3).
 */
@Composable
private fun AnnouncementThumbnail(
    category: AnnouncementCategory,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme

    Box(
        modifier = modifier
            .size(UthDimens.Size.Thumbnail)
            .clip(MaterialTheme.shapes.small)
            .background(
                if (category == AnnouncementCategory.YOUTH_UNION) {
                    colors.secondaryContainer
                } else {
                    colors.primaryContainer
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = category.icon(),
            contentDescription = null,
            tint = colors.primary,
            modifier = Modifier.size(UthDimens.Size.Icon),
        )
    }
}

/**
 * Dòng nguồn: đơn vị (nhấn) • nơi đăng hoặc ngày đăng.
 * Một màu phụ duy nhất để không nhiễu.
 */
@Composable
private fun AnnouncementMeta(
    publisher: String,
    trailing: String,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val secondary = UthTheme.extendedColors.textSecondary

    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = publisher,
            style = MaterialTheme.typography.labelSmall,
            color = colors.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        Text(
            text = stringResource(R.string.common_meta_separator),
            style = MaterialTheme.typography.labelSmall,
            color = secondary,
            modifier = Modifier.padding(horizontal = UthDimens.Spacing.Xs),
        )
        Text(
            text = trailing,
            style = MaterialTheme.typography.labelSmall,
            color = secondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Chân bảng tin: số thông báo đang hiện + lối vào kho lưu trữ (một CTA duy nhất). */
@Composable
private fun HomeFooter(
    total: Int,
    onOpenArchive: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme

    UthCard(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = pluralStringResource(R.plurals.home_feed_count, total, total),
                style = MaterialTheme.typography.bodySmall,
                color = UthTheme.extendedColors.textSecondary,
                modifier = Modifier.weight(1f),
            )
            Row(
                modifier = Modifier
                    .height(UthDimens.Size.MinTouchTarget)
                    .clickable(role = Role.Button, onClick = onOpenArchive)
                    .padding(start = UthDimens.Spacing.Sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.home_archive),
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.primary,
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(UthDimens.Size.IconBadge),
                )
            }
        }
    }
}

/**
 * Skeleton theo mục 4.2: khối `SurfaceSubtle` đúng kích thước nội dung thật (ảnh bìa +
 * 3 dòng tin) để layout không nhảy khi dữ liệu về, cộng shimmer `Motion.ShimmerMillis`.
 * Nhịp shimmer do cha giữ để mọi khối sáng cùng lúc thay vì lệch pha. Nhãn cho TalkBack
 * để cả khối không thành một vùng vô nghĩa.
 */
@Composable
private fun HomeLoading(modifier: Modifier = Modifier) {
    val label = stringResource(R.string.home_loading)
    val shimmer = rememberInfiniteTransition(label = "homeSkeleton")
    val shimmerProgress by shimmer.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = UthDimens.Motion.ShimmerMillis,
                easing = LinearEasing,
            ),
            repeatMode = RepeatMode.Restart,
        ),
        label = "homeSkeletonProgress",
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = label }
            .padding(
                start = UthDimens.Spacing.Xl,
                end = UthDimens.Spacing.Xl,
                top = UthDimens.Spacing.Xl,
                bottom = UthDimens.Spacing.Xl,
            ),
    ) {
        SkeletonBlock(
            progress = shimmerProgress,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(CoverAspectRatio),
        )
        Spacer(Modifier.height(UthDimens.Spacing.Xl))
        repeat(SkeletonRowCount) { index ->
            if (index > 0) Spacer(Modifier.height(UthDimens.Spacing.Lg))
            Row(modifier = Modifier.fillMaxWidth()) {
                SkeletonBlock(
                    progress = shimmerProgress,
                    modifier = Modifier.size(UthDimens.Size.Thumbnail),
                )
                Spacer(Modifier.width(UthDimens.Spacing.Md))
                Column(modifier = Modifier.weight(1f)) {
                    SkeletonBlock(
                        progress = shimmerProgress,
                        modifier = Modifier
                            .fillMaxWidth(SkeletonTitleFraction)
                            .height(UthDimens.Spacing.Md),
                    )
                    Spacer(Modifier.height(UthDimens.Spacing.Sm))
                    SkeletonBlock(
                        progress = shimmerProgress,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(UthDimens.Spacing.Md),
                    )
                    Spacer(Modifier.height(UthDimens.Spacing.Sm))
                    SkeletonBlock(
                        progress = shimmerProgress,
                        modifier = Modifier
                            .fillMaxWidth(SkeletonMetaFraction)
                            .height(UthDimens.Spacing.Sm),
                    )
                }
            }
        }
    }
}

/**
 * Khối giữ chỗ có shimmer (mục 4.2): nền `SurfaceSubtle` cộng một dải sáng `Surface` chạy
 * ngang. Cả hai lớp vẽ trong [drawBehind] — đặt `.background()` trước nó thì dải sáng bị
 * nền che mất.
 *
 * @param progress 0f → 1f, do [HomeLoading] giữ để các khối sáng đồng nhịp.
 */
@Composable
private fun SkeletonBlock(progress: Float, modifier: Modifier = Modifier) {
    val base = UthTheme.extendedColors.surfaceSubtle
    val highlight = MaterialTheme.colorScheme.surface.copy(alpha = ShimmerHighlightAlpha)

    Box(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .drawBehind {
                val band = size.width * ShimmerBandFraction
                val travel = size.width + band * 2
                val start = -band + travel * progress

                drawRect(color = base)
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(Color.Transparent, highlight, Color.Transparent),
                        start = Offset(start, 0f),
                        end = Offset(start + band, size.height),
                    ),
                )
            },
    )
}

/** enum → chuỗi đặt ở UI: ViewModel không được cầm `@StringRes`. */
@Composable
private fun HomeError(onRetry: () -> Unit, modifier: Modifier = Modifier) {
    UthEmptyState(
        title = stringResource(R.string.home_error_title),
        body = stringResource(R.string.home_error_body),
        icon = Icons.Filled.Warning,
        actionLabel = stringResource(R.string.common_retry),
        onAction = onRetry,
        modifier = modifier,
    )
}

@Composable
private fun HomeEmpty(modifier: Modifier = Modifier) {
    UthEmptyState(
        title = stringResource(R.string.home_empty_title),
        body = stringResource(R.string.home_empty_body),
        icon = Icons.Filled.Notifications,
        modifier = modifier,
    )
}

/** Khoá của các item trong `LazyColumn` — không dùng chuỗi rải rác trong code. */
private object HomeKey {
    const val Header = "home_header"
    const val Highlights = "home_highlights"
    const val FeedHeader = "home_feed_header"
    const val Categories = "home_categories"
    const val FeedList = "home_feed_list"
    const val Empty = "home_empty"
    const val Footer = "home_footer"
    const val Loading = "home_loading"
    const val Error = "home_error"
}

/** Key của chip "Tất cả" — tách khỏi key item khác của `LazyRow`. */
private const val CategoryAllKey = "home_category_all"

/** Bề rộng thẻ banner theo phần bề rộng màn hình; phần còn lại để thẻ kế tiếp "ló" ra. */
private const val BannerWidthFraction = 0.86f

/** Ảnh bìa 16:9 theo mục 3.3. */
private const val CoverAspectRatio = 16f / 9f

/** Alpha của chữ phụ trên nền `primary` — vẫn ≥ 4.5:1 sau khi trộn với nền Navy. */
private const val HeaderMetaAlpha = 0.78f

/** Nền nút icon trên header: đủ tách khỏi Navy, không thành khối trắng chói. */
private const val HeaderButtonFillAlpha = 0.14f
private const val HeaderButtonBorderAlpha = 0.22f

/** Vòng trang trí trên header: bán kính theo bề rộng khối, alpha rất thấp. */
private const val HeaderRingRadiusFraction = 0.42f
private const val HeaderRingStrokeFraction = 0.055f
private const val HeaderRingAlpha = 0.06f
private const val HeaderRingGoldAlpha = 0.10f

/** Icon nhóm chìm trên ảnh bìa — đủ thấy là "chưa có ảnh", không tranh với badge. */
private const val CoverGlyphAlpha = 0.28f

/** Chip trên ảnh bìa: nền `surface` gần đặc để tương phản không phụ thuộc màu ảnh. */
private const val CoverChipAlpha = 0.92f

private const val SkeletonRowCount = 3
private const val SkeletonTitleFraction = 0.62f
private const val SkeletonMetaFraction = 0.38f

/** Bề rộng dải sáng shimmer, tính theo bề rộng khối. */
private const val ShimmerBandFraction = 0.6f

/** Độ đậm dải sáng: đủ thấy chuyển động, không thành hiệu ứng nháy. */
private const val ShimmerHighlightAlpha = 0.7f

/** Định dạng ngày theo locale trường: `dd/MM/yyyy`. */
private val FeedDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

/** Nhãn nhóm trong `strings.xml`; prefix `home_` giữ đúng quy ước mục 8. */
@StringRes
private fun AnnouncementCategory.labelRes(): Int = when (this) {
    AnnouncementCategory.TRAINING -> R.string.home_category_training
    AnnouncementCategory.STUDENT_AFFAIRS -> R.string.home_category_student_affairs
    AnnouncementCategory.TUITION -> R.string.home_category_tuition
    AnnouncementCategory.YOUTH_UNION -> R.string.home_category_youth_union
}

/** Icon đại diện khi chưa có ảnh thật; bộ `material-icons-core` đã đóng băng (mục 7). */
private fun AnnouncementCategory.icon(): ImageVector = when (this) {
    AnnouncementCategory.TRAINING -> Icons.Filled.DateRange
    AnnouncementCategory.STUDENT_AFFAIRS -> Icons.Filled.Person
    AnnouncementCategory.TUITION -> Icons.Filled.ShoppingCart
    AnnouncementCategory.YOUTH_UNION -> Icons.Filled.Star
}

private val PreviewAnnouncements = listOf(
    Announcement(
        id = "preview-1",
        category = AnnouncementCategory.TRAINING,
        title = "Kế hoạch đăng ký học phần học kỳ mới",
        summary = "Cổng đăng ký mở từ 08:00 ngày 01/11 cho sinh viên khóa 2023 - 2025.",
        publisher = "Phòng Đào tạo",
        publishedAt = LocalDateTime.of(2026, 10, 29, 8, 30),
        isUnread = true,
    ),
    Announcement(
        id = "preview-2",
        category = AnnouncementCategory.STUDENT_AFFAIRS,
        title = "Chính sách hỗ trợ sinh viên có hoàn cảnh khó khăn",
        summary = "Hướng dẫn nộp hồ sơ xét trợ cấp xã hội đợt 1.",
        publisher = "Phòng CTSV",
        publishedAt = LocalDateTime.of(2026, 10, 27, 14, 0),
        source = "Cơ sở 1 & 2",
    ),
    Announcement(
        id = "preview-3",
        category = AnnouncementCategory.YOUTH_UNION,
        title = "Cập nhật hoạt động sinh viên sắp diễn ra",
        summary = "Ghi nhận điểm rèn luyện cho sinh viên tham gia Hội thao UTH.",
        publisher = "Đoàn Trường UTH",
        publishedAt = LocalDateTime.of(2026, 10, 24, 11, 0),
        source = "Cộng điểm ĐRL",
    ),
)

private val PreviewBanners = listOf(
    HomeBanner(
        announcement = PreviewAnnouncements.first(),
        documentCode = "108/TB-ĐHGTVT",
        isMandatory = true,
    ),
    HomeBanner(
        announcement = PreviewAnnouncements[1],
        documentCode = "92/TB-CTSV",
    ),
    HomeBanner(announcement = PreviewAnnouncements[2]),
)

@Composable
private fun HomeStatePreview(state: HomeUiState, darkTheme: Boolean = false) =
    UthPreview(darkTheme = darkTheme) { HomeScreen(uiState = state, onAction = {}) }

@Preview(name = "content light", showBackground = true)
@Composable
private fun HomeContentPreview() = HomeStatePreview(
    HomeUiState(isLoading = false, banners = PreviewBanners, announcements = PreviewAnnouncements),
)

@Preview(name = "content dark", showBackground = true)
@Composable
private fun HomeContentDarkPreview() = HomeStatePreview(
    HomeUiState(isLoading = false, banners = PreviewBanners, announcements = PreviewAnnouncements),
    darkTheme = true,
)

@Preview(name = "loading light", showBackground = true)
@Composable
private fun HomeLoadingPreview() = HomeStatePreview(HomeUiState(isLoading = true))

@Preview(name = "loading dark", showBackground = true)
@Composable
private fun HomeLoadingDarkPreview() = HomeStatePreview(HomeUiState(isLoading = true), darkTheme = true)

@Preview(name = "error light", showBackground = true)
@Composable
private fun HomeErrorPreview() = HomeStatePreview(
    HomeUiState(isLoading = false, failure = HomeFailure.LoadFailed),
)

@Preview(name = "error dark", showBackground = true)
@Composable
private fun HomeErrorDarkPreview() = HomeStatePreview(
    HomeUiState(isLoading = false, failure = HomeFailure.LoadFailed),
    darkTheme = true,
)

@Preview(name = "empty light", showBackground = true)
@Composable
private fun HomeEmptyPreview() = HomeStatePreview(HomeUiState(isLoading = false))

@Preview(name = "empty dark", showBackground = true)
@Composable
private fun HomeEmptyDarkPreview() = HomeStatePreview(HomeUiState(isLoading = false), darkTheme = true)

/** Font scale 130 % — kiểm tra layout không tràn/cắt chữ (`docs/Design-system.md` mục 8). */
@Preview(name = "content fontScale 130%", showBackground = true, fontScale = 1.3f)
@Composable
private fun HomeContentFontScalePreview() = HomeStatePreview(
    HomeUiState(isLoading = false, banners = PreviewBanners, announcements = PreviewAnnouncements),
)
