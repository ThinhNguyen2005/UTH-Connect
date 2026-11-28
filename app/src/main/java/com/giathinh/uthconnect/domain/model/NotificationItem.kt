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
    INCIDENT_SUBMITTED,
    INCIDENT_STATUS_UPDATED,
    INCIDENT_RESOLVED,
    INCIDENT_REJECTED,
    SYSTEM,
    ;

    companion object {
        @Suppress("MaxLineLength")
        fun fromString(value: String?): UserNotificationType = entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: SYSTEM
    }
}
