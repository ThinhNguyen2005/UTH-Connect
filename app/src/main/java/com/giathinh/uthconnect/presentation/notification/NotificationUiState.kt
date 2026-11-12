package com.giathinh.uthconnect.presentation.notification

import com.giathinh.uthconnect.domain.model.NotificationCategory
import com.giathinh.uthconnect.domain.model.PostNotification

/**
 * Trạng thái giao diện màn hình Thông Báo (immutable UiState).
 */
data class NotificationUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val selectedCategory: NotificationCategory? = null,
    val notifications: List<PostNotification> = emptyList(),
    val readNotificationIds: Set<String> = emptySet(),
    val errorMessage: String? = null,
    val detailId: String? = null,
    val detail: PostNotification? = null,
    val isDetailLoading: Boolean = false,
) {
    /**
     * Danh sách thông báo sau khi lọc theo danh mục đang chọn.
     */
    val filteredNotifications: List<PostNotification>
        get() = if (selectedCategory == null || selectedCategory == NotificationCategory.ALL) {
            notifications
        } else {
            notifications.filter { it.category == selectedCategory }
        }

    /**
     * Danh sách thông báo nhóm "Hôm nay" (hoặc mới nhất).
     */
    val todayNotifications: List<PostNotification>
        get() = filteredNotifications.filter { isToday(it.publishedAt) }

    /**
     * Danh sách thông báo nhóm "Hôm qua & Trước đó".
     */
    val previousNotifications: List<PostNotification>
        get() = filteredNotifications.filterNot { isToday(it.publishedAt) }

    /**
     * Số lượng thông báo chưa đọc trong nhóm "Hôm nay".
     */
    val todayNewCount: Int
        get() = todayNotifications.count { it.id !in readNotificationIds }

    /**
     * Tổng số thông báo chưa đọc toàn bộ.
     */
    val totalUnreadCount: Int
        get() = notifications.count { it.id !in readNotificationIds }

    fun countForCategory(category: NotificationCategory?): Int {
        return if (category == null || category == NotificationCategory.ALL) {
            notifications.size
        } else {
            notifications.count { it.category == category }
        }
    }

    private fun isToday(timestampMs: Long): Boolean {
        // Coi như hôm nay nếu trong vòng 24 giờ kể từ thời điểm hiện tại hoặc timestamp lớn hơn
        val now = System.currentTimeMillis()
        val diff = now - timestampMs
        return diff in 0..(24 * 3600 * 1000L)
    }
}
