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
}
