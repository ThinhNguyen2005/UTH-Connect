package com.giathinh.uthconnect.data.repository

import com.giathinh.uthconnect.domain.model.NotificationItem
import com.giathinh.uthconnect.domain.model.UserNotificationType
import com.giathinh.uthconnect.domain.repository.UserNotificationRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
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
    private val countMutex = Mutex()
    private val badgeRefreshScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val badgeRefreshLock = Any()
    private var badgeRefreshJob: Job? = null
    private var countOwnerId: String? = null

    override val unreadCount = mutableUnreadCount.asStateFlow()
    override val currentUserId: String? get() = client.userIdOrNull

    override fun getNotifications(): Flow<List<NotificationItem>> = flow {
        val userId = urlEncode(uuid(client.userId))
        val rows = buildList {
            var cursorCreatedAt: String? = null
            var cursorId: String? = null
            while (true) {
                val cursorFilter = if (cursorCreatedAt != null && cursorId != null) {
                    val createdAt = urlEncode(requireNotNull(cursorCreatedAt))
                    val id = urlEncode(requireNotNull(cursorId))
                    "or(created_at.lt.$createdAt,and(created_at.eq.$createdAt,id.lt.$id))"
                } else {
                    null
                }
                val ownershipFilter =
                    "or(user_id.eq.$userId,recipient_id.eq.$userId,and(user_id.is.null,recipient_id.is.null))"
                val filters = cursorFilter?.let { "&and=($ownershipFilter,$it)" }
                    ?: "&or=(user_id.eq.$userId,recipient_id.eq.$userId,and(user_id.is.null,recipient_id.is.null))"
                val page = client.table(
                    "notifications?select=id,user_id,recipient_id,title,content,type,is_read,created_at" +
                        filters + "&order=created_at.desc,id.desc&limit=$USER_NOTIFICATION_PAGE_SIZE",
                ).jsonArray.map { it.jsonObject }
                addAll(page)
                if (page.size < USER_NOTIFICATION_PAGE_SIZE) break
                val last = page.last()
                cursorCreatedAt = requireNotNull(last.string("created_at"))
                cursorId = requireNotNull(last.string("id"))
            }
        }
        emit(rows.map { it.toNotificationItem() })
    }

    override suspend fun refreshUnreadCount(): Result<Int> = countMutex.withLock {
        suspendResult {
            val rawUserId = uuid(client.userId)
            val userId = urlEncode(rawUserId)
            resetCountForUser(userId)
            val count = client.rpc("count_unread_user_notifications", buildJsonObject {})
                .jsonPrimitive.content.toInt()
            if (countOwnerId == userId && runCatching { uuid(client.userId) == rawUserId }.getOrDefault(false)) {
                mutableUnreadCount.value = count
            }
            count
        }
    }

    override fun scheduleUnreadCountRefresh() {
        val expectedUserId = currentUserId ?: return
        synchronized(badgeRefreshLock) {
            badgeRefreshJob?.cancel()
            badgeRefreshJob = badgeRefreshScope.launch {
                if (currentUserId == expectedUserId) refreshUnreadCount()
            }
        }
    }

    override fun clearUnreadCount() {
        synchronized(badgeRefreshLock) {
            badgeRefreshJob?.cancel()
            badgeRefreshJob = null
            countOwnerId = null
            mutableUnreadCount.value = 0
        }
    }

    override suspend fun markAsRead(notificationId: String): Result<Boolean> = countMutex.withLock {
        suspendResult {
            val rawUserId = uuid(client.userId)
            val userId = urlEncode(rawUserId)
            val id = urlEncode(uuid(notificationId))
            val changed = client.patchTable(
                "notifications?id=eq.$id&is_read=eq.false&or=(user_id.eq.$userId,recipient_id.eq.$userId)",
                buildJsonObject { put("is_read", true) },
            ).jsonArray.isNotEmpty()
            val stillSameUser = runCatching { uuid(client.userId) == rawUserId }.getOrDefault(false)
            if (changed && countOwnerId == userId && stillSameUser) {
                mutableUnreadCount.value = (mutableUnreadCount.value - 1).coerceAtLeast(0)
            }
            changed
        }
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
