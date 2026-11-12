package com.giathinh.uthconnect.presentation.notification

import com.giathinh.uthconnect.domain.model.NotificationCategory

/**
 * Các hành động từ người dùng trên màn hình Thông Báo (MVI).
 */
sealed interface NotificationAction {
    /** Quay lại màn hình trước. */
    data object Back : NotificationAction

    /** Đánh dấu tất cả thông báo là đã đọc. */
    data object MarkAllAsRead : NotificationAction

    /** Mở cài đặt thông báo. */
    data object OpenSettings : NotificationAction

    /** Mở trang cá nhân. */
    data object OpenProfile : NotificationAction

    /** Chọn một tab danh mục bộ lọc. Null = Tất cả. */
    data class SelectCategory(val category: NotificationCategory?) : NotificationAction

    /** Bấm vào một thông báo để đọc chi tiết và đánh dấu đã đọc. */
    data class ClickNotification(val notificationId: String) : NotificationAction

    /** Làm mới danh sách thông báo. */
    data object Refresh : NotificationAction

    /** Loads a notification opened directly from an FCM tap. */
    data class LoadDetail(val notificationId: String) : NotificationAction
}
