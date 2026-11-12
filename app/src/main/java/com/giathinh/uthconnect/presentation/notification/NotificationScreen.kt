package com.giathinh.uthconnect.presentation.notification

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.giathinh.uthconnect.R
import com.giathinh.uthconnect.core.designsystem.theme.SuccessContainerLight
import com.giathinh.uthconnect.core.designsystem.theme.SuccessOnContainerLight
import com.giathinh.uthconnect.core.designsystem.theme.UthDimens
import com.giathinh.uthconnect.core.designsystem.theme.UthNavy
import com.giathinh.uthconnect.core.designsystem.theme.UthNavyContainerLight
import com.giathinh.uthconnect.core.designsystem.theme.UthOnNavyContainerLight
import com.giathinh.uthconnect.domain.model.NotificationCategory
import com.giathinh.uthconnect.domain.model.PostNotification
import java.util.Date
import java.util.Locale

/**
 * Màn hình Thông Báo (Notifications Screen) chuẩn Material 3 & UTH Design System.
 * Thiết kế giao diện pixel-perfect theo HTML/DESIGN spec và đính kèm của người dùng.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreen(
    uiState: NotificationUiState,
    onAction: (NotificationAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            NotificationTopBar(
                onBack = { onAction(NotificationAction.Back) },
                onMarkAllRead = { onAction(NotificationAction.MarkAllAsRead) },
                onOpenSettings = { onAction(NotificationAction.OpenSettings) },
                onOpenProfile = { onAction(NotificationAction.OpenProfile) },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            // Thanh bộ lọc dạng Chip ngang
            NotificationFilterChipsRow(
                uiState = uiState,
                onSelectCategory = { onAction(NotificationAction.SelectCategory(it)) },
            )

            if (uiState.detailId != null) {
                when {
                    uiState.isDetailLoading -> NotificationLoadingState()
                    uiState.detail != null -> NotificationDetailContent(uiState.detail!!)
                    else -> NotificationDetailUnavailable()
                }
            } else if (uiState.isLoading) {
                NotificationLoadingState()
            } else if (uiState.errorMessage != null && uiState.notifications.isEmpty()) {
                NotificationErrorState(
                    message = uiState.errorMessage,
                    onRetry = { onAction(NotificationAction.Refresh) },
                )
            } else if (uiState.filteredNotifications.isEmpty()) {
                NotificationEmptyState()
            } else {
                NotificationFeed(
                    uiState = uiState,
                    onItemClick = { onAction(NotificationAction.ClickNotification(it)) },
                )
            }
        }
    }
}

/**
 * Top App Bar với nút Quay lại, Tiêu đề, Đánh dấu đã đọc, Cài đặt và Avatar tròn.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotificationTopBar(
    onBack: () -> Unit,
    onMarkAllRead: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenProfile: () -> Unit,
) {
    TopAppBar(
        title = {
            Text(
                text = stringResource(R.string.notification_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
            )
        },
        navigationIcon = {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(UthDimens.Size.MinTouchTarget),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.notification_back),
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        },
        actions = {
            IconButton(
                onClick = onMarkAllRead,
                modifier = Modifier.size(UthDimens.Size.MinTouchTarget),
            ) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = stringResource(R.string.notification_mark_all_read),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier.size(UthDimens.Size.MinTouchTarget),
            ) {
                Icon(
                    imageVector = Icons.Filled.Build,
                    contentDescription = stringResource(R.string.notification_settings),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            // User Avatar Profile Pill
            Box(
                modifier = Modifier
                    .padding(end = UthDimens.Spacing.Sm)
                    .size(UthDimens.Spacing.Xxl)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable(onClick = onOpenProfile),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = stringResource(R.string.notification_profile),
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(UthDimens.Size.IconInline),
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    )
}

/**
 * Hàng filter chips cuộn ngang: Tất cả, Học tập & TKB, Sự kiện, Đấu giá, Hệ thống.
 */
@Composable
private fun NotificationFilterChipsRow(
    uiState: NotificationUiState,
    onSelectCategory: (NotificationCategory?) -> Unit,
) {
    val scrollState = rememberScrollState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(horizontal = UthDimens.Spacing.Lg, vertical = UthDimens.Spacing.Sm),
        horizontalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Chip 1: Tất cả
        NotificationFilterChip(
            label = stringResource(R.string.notification_category_all, uiState.countForCategory(null)),
            isSelected = uiState.selectedCategory == null || uiState.selectedCategory == NotificationCategory.ALL,
            onClick = { onSelectCategory(null) },
        )

        // Chip 2: Học tập & TKB — hiển thị cả ACADEMIC + EXAMINATION
        NotificationFilterChip(
            label = stringResource(
                R.string.notification_category_study,
                uiState.countForCategory(NotificationCategory.ACADEMIC),
            ),
            isSelected = uiState.selectedCategory == NotificationCategory.ACADEMIC,
            onClick = { onSelectCategory(NotificationCategory.ACADEMIC) },
        )

        // Chip 3: Sự kiện
        NotificationFilterChip(
            label = stringResource(R.string.notification_category_event, uiState.countForCategory(NotificationCategory.ACTIVITIES)),
            isSelected = uiState.selectedCategory == NotificationCategory.ACTIVITIES,
            onClick = { onSelectCategory(NotificationCategory.ACTIVITIES) },
        )

        // Chip 4: Hệ thống — hiển thị cả GENERAL + TUITION
        NotificationFilterChip(
            label = stringResource(R.string.notification_category_system, uiState.countForCategory(NotificationCategory.GENERAL)),
            isSelected = uiState.selectedCategory == NotificationCategory.GENERAL,
            onClick = { onSelectCategory(NotificationCategory.GENERAL) },
        )
    }
}

@Composable
private fun NotificationLoadingState() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            modifier = Modifier.size(UthDimens.Size.Progress),
            strokeWidth = UthDimens.Size.ProgressStroke,
            color = UthNavy,
        )
    }
}

/**
 * Trạng thái lỗi kèm nút thử lại — hiển thị khi tải inbox thất bại và chưa có dữ liệu.
 */
@Composable
private fun NotificationErrorState(
    message: String,
    onRetry: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(UthDimens.Spacing.Xl),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Md),
        ) {
            Icon(
                imageVector = Icons.Filled.Notifications,
                contentDescription = null,
                modifier = Modifier.size(UthDimens.Size.AvatarLarge),
                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
            )
            Text(
                text = stringResource(R.string.notification_error_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(onClick = onRetry) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(UthDimens.Size.IconBadge),
                )
                Spacer(modifier = Modifier.size(UthDimens.Spacing.Xs))
                Text(text = stringResource(R.string.notification_retry))
            }
        }
    }
}

@Composable
private fun NotificationDetailContent(item: PostNotification) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(UthDimens.Spacing.Lg),
        verticalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Md),
    ) {
        NotificationCategoryTag(category = item.category, isSuccess = false)
        Text(item.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
            text = formatNotificationTime(item.publishedAt),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (item.summary.isNotBlank()) {
            Text(item.summary, style = MaterialTheme.typography.titleMedium)
        }
        Text(item.content, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun NotificationDetailUnavailable() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = stringResource(R.string.notification_detail_unavailable),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Filter Chip đơn lẻ chuẩn Material 3 pill shape.
 */
@Composable
private fun NotificationFilterChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = UthDimens.Radius.PillShape,
        color = if (isSelected) UthNavyContainerLight else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (isSelected) UthOnNavyContainerLight else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.height(UthDimens.Size.ChipHeight),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = UthDimens.Spacing.Md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    modifier = Modifier
                        .size(UthDimens.Size.IconBadge)
                        .padding(end = UthDimens.Spacing.Xs),
                    tint = UthNavy,
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
            )
        }
    }
}

/**
 * Danh sách thông báo chia 2 nhóm: "Hôm nay" và "Hôm qua & Trước đó".
 */
@Composable
private fun NotificationFeed(
    uiState: NotificationUiState,
    onItemClick: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            horizontal = UthDimens.Spacing.Lg,
            vertical = UthDimens.Spacing.Sm,
        ),
        verticalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Md),
    ) {
        // Nhóm 1: Hôm nay
        if (uiState.todayNotifications.isNotEmpty()) {
            item(key = "header_today") {
                NotificationSectionHeader(
                    title = stringResource(R.string.notification_section_today),
                    badgeText = if (uiState.todayNewCount > 0) {
                        stringResource(R.string.notification_new_count, uiState.todayNewCount)
                    } else null,
                )
            }

            items(
                items = uiState.todayNotifications,
                key = { "today_${it.id}" },
            ) { item ->
                val isUnread = item.id !in uiState.readNotificationIds
                NotificationCardItem(
                    item = item,
                    isUnread = isUnread,
                    onClick = { onItemClick(item.id) },
                )
            }
        }

        // Nhóm 2: Hôm qua & Trước đó
        if (uiState.previousNotifications.isNotEmpty()) {
            item(key = "header_previous") {
                Spacer(modifier = Modifier.height(UthDimens.Spacing.Sm))
                NotificationSectionHeader(
                    title = stringResource(R.string.notification_section_previous),
                    badgeText = stringResource(R.string.notification_viewed),
                    isMutedBadge = true,
                )
            }

            items(
                items = uiState.previousNotifications,
                key = { "prev_${it.id}" },
            ) { item ->
                val isUnread = item.id !in uiState.readNotificationIds
                NotificationCardItem(
                    item = item,
                    isUnread = isUnread,
                    onClick = { onItemClick(item.id) },
                )
            }
        }
    }
}

/**
 * Header tiêu đề nhóm ("HÔM NAY", "HÔM QUA & TRƯỚC ĐÓ") kèm badge phụ.
 */
@Composable
private fun NotificationSectionHeader(
    title: String,
    badgeText: String?,
    isMutedBadge: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = UthDimens.Spacing.Xs),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (badgeText != null) {
            Surface(
                shape = UthDimens.Radius.PillShape,
                color = if (isMutedBadge) Color.Transparent else UthNavyContainerLight,
                contentColor = if (isMutedBadge) MaterialTheme.colorScheme.onSurfaceVariant else UthNavy,
            ) {
                Text(
                    text = badgeText,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(
                        horizontal = UthDimens.Spacing.Sm,
                        vertical = UthDimens.Spacing.Xxs,
                    ),
                )
            }
        }
    }
}

/**
 * Thẻ thông báo chi tiết: Icon theo category, Tag phân loại, Thời gian, Dấu chấm chưa đọc, Tiêu đề & Nội dung tóm tắt.
 */
@Composable
private fun NotificationCardItem(
    item: PostNotification,
    isUnread: Boolean,
    onClick: () -> Unit,
) {
    val isSuccessEvent = item.category == NotificationCategory.ACTIVITIES || item.title.contains("Check-in", ignoreCase = true)
    val cardBackground = if (isUnread) {
        MaterialTheme.colorScheme.surface
    } else {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
    }

    Surface(
        onClick = onClick,
        shape = UthDimens.Radius.MdShape,
        color = cardBackground,
        border = androidx.compose.foundation.BorderStroke(
            width = UthDimens.Size.Stroke,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (isUnread) 0.5f else 0.25f),
        ),
        tonalElevation = if (isUnread) UthDimens.Elevation.Card else UthDimens.Elevation.None,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(UthDimens.Spacing.Md),
            horizontalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Md),
            verticalAlignment = Alignment.Top,
        ) {
            // Icon container bên trái (36dp)
            NotificationIconBadge(
                category = item.category,
                isSuccess = isSuccessEvent,
            )

            // Cột nội dung chính
            Column(
                modifier = Modifier.weight(1f),
            ) {
                // Hàng tag danh mục và thời gian / chấm xanh chưa đọc
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    NotificationCategoryTag(
                        category = item.category,
                        isSuccess = isSuccessEvent,
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Xs),
                    ) {
                        Text(
                            text = formatNotificationTime(item.publishedAt),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )

                        // Chấm xanh thông báo chưa đọc
                        if (isUnread) {
                            Box(
                                modifier = Modifier
                                    .size(UthDimens.Size.IndicatorDot)
                                    .clip(CircleShape)
                                    .background(UthNavy),
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(UthDimens.Spacing.Xs))

                // Tiêu đề thông báo
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = if (isUnread) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                // Nội dung tóm tắt
                if (item.summary.isNotBlank()) {
                    Spacer(modifier = Modifier.height(UthDimens.Spacing.Xxs))
                    Text(
                        text = item.summary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/**
 * Icon container vuông mềm (36dp) phân màu theo danh mục thông báo.
 */
@Composable
private fun NotificationIconBadge(
    category: NotificationCategory,
    isSuccess: Boolean,
) {
    val bgColor: Color
    val iconColor: Color
    val iconVector: ImageVector

    when {
        isSuccess -> {
            bgColor = SuccessContainerLight
            iconColor = SuccessOnContainerLight
            iconVector = Icons.Filled.CheckCircle
        }
        category == NotificationCategory.ACADEMIC || category == NotificationCategory.EXAMINATION -> {
            bgColor = MaterialTheme.colorScheme.surfaceVariant
            iconColor = MaterialTheme.colorScheme.onSurfaceVariant
            iconVector = Icons.Filled.DateRange
        }
        category == NotificationCategory.GENERAL -> {
            bgColor = MaterialTheme.colorScheme.surfaceVariant
            iconColor = MaterialTheme.colorScheme.onSurfaceVariant
            iconVector = Icons.Filled.Notifications
        }
        else -> {
            bgColor = MaterialTheme.colorScheme.surfaceVariant
            iconColor = MaterialTheme.colorScheme.onSurfaceVariant
            iconVector = Icons.Filled.Notifications
        }
    }

    Box(
        modifier = Modifier
            .size(UthDimens.Size.NotificationIconContainer)
            .clip(UthDimens.Radius.SmShape)
            .background(bgColor),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = iconVector,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(UthDimens.Size.IconInline),
        )
    }
}

/**
 * Tag hiển thị tên danh mục/đơn vị (ví dụ: "✓ Sự kiện UTH", "Đào tạo", "Hệ thống").
 */
@Composable
private fun NotificationCategoryTag(
    category: NotificationCategory,
    isSuccess: Boolean,
) {
    val tagText = when (category) {
        NotificationCategory.ACTIVITIES -> stringResource(R.string.notification_tag_event)
        NotificationCategory.ACADEMIC, NotificationCategory.EXAMINATION -> stringResource(R.string.notification_tag_academic)
        NotificationCategory.TUITION, NotificationCategory.GENERAL -> stringResource(R.string.notification_tag_system)
        NotificationCategory.ALL -> stringResource(R.string.notification_tag_generic)
    }

    val tagBg = if (isSuccess) SuccessContainerLight else MaterialTheme.colorScheme.surfaceVariant
    val tagColor = if (isSuccess) SuccessOnContainerLight else MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        shape = UthDimens.Radius.SmShape,
        color = tagBg,
        contentColor = tagColor,
    ) {
        Text(
            text = tagText,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(
                horizontal = UthDimens.Spacing.Xs,
                vertical = UthDimens.Spacing.Xxs,
            ),
        )
    }
}

/**
 * Màn hình rỗng khi không có thông báo nào.
 */
@Composable
private fun NotificationEmptyState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(UthDimens.Spacing.Xl),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Notifications,
                contentDescription = null,
                modifier = Modifier.size(UthDimens.Size.AvatarLarge),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            )
            Spacer(modifier = Modifier.height(UthDimens.Spacing.Lg))
            Text(
                text = stringResource(R.string.notification_empty_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(UthDimens.Spacing.Sm))
            Text(
                text = stringResource(R.string.notification_empty_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Định dạng thời gian hiển thị: "08:30 hôm nay", "Hôm qua", hoặc "dd/MM/yyyy".
 */
private fun formatNotificationTime(timestampMs: Long): String {
    return java.text.DateFormat.getDateTimeInstance(
        java.text.DateFormat.MEDIUM,
        java.text.DateFormat.SHORT,
        Locale.getDefault(),
    ).format(Date(timestampMs))
}
