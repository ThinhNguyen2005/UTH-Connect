package com.giathinh.uthconnect.domain.repository

import com.giathinh.uthconnect.domain.model.NotificationCategory
import com.giathinh.uthconnect.domain.model.PostNotification
import kotlinx.coroutines.flow.Flow

/**
 * Interface Repository quản lý Bảng tin & Thông báo Nhà trường.
 * Phục vụ KAN-28 (Đức Dịch Viết).
 */
interface NotificationRepository {
    /** Lấy newsfeed bảng tin, có thể lọc theo danh mục */
    fun getNewsfeed(category: NotificationCategory = NotificationCategory.ALL): Flow<List<PostNotification>>

    /** Lấy chi tiết một thông báo */
    suspend fun getNotificationDetail(notificationId: String): Result<PostNotification?>

    /** Loads the signed-in user's persistent notification inbox. */
    fun getInbox(): Flow<List<PostNotification>> = getNewsfeed(NotificationCategory.ALL)

    /** Marks one inbox notification as read. */
    suspend fun markInboxNotificationRead(notificationId: String): Result<Unit> = Result.success(Unit)

    /** Marks all inbox notifications as read. */
    suspend fun markAllInboxNotificationsRead(): Result<Unit> = Result.success(Unit)

    /** Loads one persistent inbox item for notification-tap navigation. */
    suspend fun getInboxNotification(notificationId: String): Result<PostNotification?> =
        getNotificationDetail(notificationId)
}
