package com.giathinh.uthconnect.presentation.notification

import com.giathinh.uthconnect.domain.model.NotificationCategory
import com.giathinh.uthconnect.domain.model.PostNotification
import java.util.Calendar

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
     * Chip "Học tập & TKB" → ACADEMIC + EXAMINATION
     * Chip "Hệ thống" → GENERAL + TUITION
     */
    val filteredNotifications: List<PostNotification>
        get() = when (selectedCategory) {
            null, NotificationCategory.ALL -> notifications

            NotificationCategory.ACADEMIC ->
                notifications.filter {
                    it.category == NotificationCategory.ACADEMIC ||
                        it.category == NotificationCategory.EXAMINATION
                }

            NotificationCategory.GENERAL ->
                notifications.filter {
                    it.category == NotificationCategory.GENERAL ||
                        it.category == NotificationCategory.TUITION
                }

            else -> notifications.filter { it.category == selectedCategory }
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

    fun countForCategory(category: NotificationCategory?): Int = when (category) {
        null, NotificationCategory.ALL -> notifications.size

        NotificationCategory.ACADEMIC ->
            notifications.count {
                it.category == NotificationCategory.ACADEMIC ||
                    it.category == NotificationCategory.EXAMINATION
            }

        NotificationCategory.GENERAL ->
            notifications.count {
                it.category == NotificationCategory.GENERAL ||
                    it.category == NotificationCategory.TUITION
            }

        else -> notifications.count { it.category == category }
    }

    private fun isToday(timestampMs: Long): Boolean {
        // So sánh ngày lịch trong cùng múi giờ thiết bị để tránh thông báo từ hôm qua
        // vẫn được xếp vào nhóm "Hôm nay" do chưa đủ 24 giờ.
        val nowCal = Calendar.getInstance()
        val tsCal = Calendar.getInstance().apply { timeInMillis = timestampMs }
        return nowCal.get(Calendar.YEAR) == tsCal.get(Calendar.YEAR) &&
            nowCal.get(Calendar.DAY_OF_YEAR) == tsCal.get(Calendar.DAY_OF_YEAR)
    }
}
