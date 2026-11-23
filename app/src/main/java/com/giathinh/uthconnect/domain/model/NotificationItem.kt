package com.giathinh.uthconnect.domain.model

/** Personal notification row from `public.notifications`. */
data class NotificationItem(
    val id: String,
    /** Normalized recipient identity, read from `user_id` or `recipient_id`. */
    val recipientId: String?,
    val title: String,
    val content: String,
    val type: UserNotificationType,
    val isRead: Boolean,
    val createdAt: Long,
)

enum class UserNotificationType {
    HOUSING_APPROVED,
    HOUSING_REJECTED,
    EVENT_REMINDER,
    SYSTEM,
    ;

    companion object {
        @Suppress("MaxLineLength")
        fun fromString(value: String?): UserNotificationType = entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: SYSTEM
    }
}
