package com.giathinh.uthconnect.data.repository

import com.giathinh.uthconnect.domain.model.NotificationItem
import com.giathinh.uthconnect.domain.model.UserNotificationType
import com.giathinh.uthconnect.domain.repository.UserNotificationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

private const val USER_NOTIFICATION_PAGE_SIZE = 500

@Singleton
class SupabasePersonalNotificationRepository @Inject constructor(
    private val client: SupabaseRestClient,
) : UserNotificationRepository {
    private val mutableUnreadCount = MutableStateFlow(0)
    private var countOwnerId: String? = null

    override val unreadCount = mutableUnreadCount.asStateFlow()

    override fun getNotifications(): Flow<List<NotificationItem>> = flow {
        val userId = urlEncode(uuid(client.userId))
        val rows = mutableListOf<JsonObject>()
        var offset = 0
        do {
            val page = client.table(
                "notifications?select=id,user_id,recipient_id,title,content,type,is_read,created_at" +
                    "&or=(user_id.eq.$userId,recipient_id.eq.$userId,recipient_id.is.null)" +
                    "&order=created_at.desc,id.desc&limit=$USER_NOTIFICATION_PAGE_SIZE&offset=$offset",
            ).jsonArray.map { it.jsonObject }
            rows += page
            offset += page.size
        } while (page.size == USER_NOTIFICATION_PAGE_SIZE)
        emit(rows.map { it.toNotificationItem() })
    }

    override suspend fun refreshUnreadCount(): Result<Int> = suspendResult {
        val userId = urlEncode(uuid(client.userId))
        resetCountForUser(userId)
        var count = 0
        var offset = 0
        do {
            val page = client.table(
                "notifications?select=id&is_read=eq.false" +
                    "&or=(user_id.eq.$userId,recipient_id.eq.$userId)" +
                    "&order=created_at.desc,id.desc&limit=$USER_NOTIFICATION_PAGE_SIZE&offset=$offset",
            ).jsonArray
            count += page.size
            offset += page.size
        } while (page.size == USER_NOTIFICATION_PAGE_SIZE)
        mutableUnreadCount.value = count
        count
    }

    override suspend fun markAsRead(notificationId: String): Result<Boolean> = suspendResult {
        val userId = urlEncode(uuid(client.userId))
        val id = urlEncode(uuid(notificationId))
        val changed = client.patchTable(
            "notifications?id=eq.$id&is_read=eq.false&or=(user_id.eq.$userId,recipient_id.eq.$userId)",
            buildJsonObject { put("is_read", true) },
        ).jsonArray.isNotEmpty()
        if (changed && countOwnerId == userId) {
            mutableUnreadCount.update { (it - 1).coerceAtLeast(0) }
        }
        changed
    }

    private fun resetCountForUser(userId: String) {
        if (countOwnerId != userId) {
            countOwnerId = userId
            mutableUnreadCount.value = 0
        }
    }

    private fun JsonObject.toNotificationItem(): NotificationItem {
        val userId = string("user_id")
        val recipientId = string("recipient_id") ?: userId
        return NotificationItem(
            id = requireNotNull(string("id")),
            recipientId = recipientId,
            title = string("title").orEmpty(),
            content = string("content").orEmpty(),
            type = UserNotificationType.fromString(string("type")),
            // Campus-wide posts have a shared is_read column, not a per-user read state.
            isRead = bool("is_read") || (userId == null && recipientId == null),
            createdAt = Instant.parse(requireNotNull(string("created_at"))).toEpochMilli(),
        )
    }
}
