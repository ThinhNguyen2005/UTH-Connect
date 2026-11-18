package com.giathinh.uthconnect.domain.repository

import com.giathinh.uthconnect.domain.model.NotificationItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface UserNotificationRepository {
    val unreadCount: StateFlow<Int>

    val currentUserId: String? get() = null

    fun getNotifications(): Flow<List<NotificationItem>>

    suspend fun refreshUnreadCount(): Result<Int>

    /** Clears account-scoped state when the signed-in navigation scope ends. */
    fun clearUnreadCount() = Unit

    /** Returns true only when this call changed an unread row. */
    suspend fun markAsRead(notificationId: String): Result<Boolean>
}
