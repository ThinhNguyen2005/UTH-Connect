package com.giathinh.uthconnect.domain.model

/**
 * Mô hình dữ liệu Sự kiện / Hoạt động CLB dành cho sinh viên UTH.
 */
data class Event(
    val id: String,
    val title: String,
    val description: String,
    val orgId: String,
    val orgName: String = "",
    val location: String,
    val campus: String,
    val startAt: Long,
    val endAt: Long,
    val capacity: Int,
    val registeredCount: Int = 0,
    val points: Int = 0,
    val status: EventStatus = EventStatus.PUBLISHED,
    val coverUrl: String? = null,
    val category: String = "",
    val managers: List<String> = emptyList(),
)

/** Data an organizer can edit. Ownership and status are decided by the server. */
data class EventDraft(
    val title: String,
    val description: String,
    val location: String,
    val campus: String,
    val startAt: Long,
    val endAt: Long,
    val capacity: Int,
    val points: Int = 0,
    val organizerName: String = "",
    val category: String = "",
    val managers: List<String> = emptyList(),
)

enum class EventStatus {
    DRAFT,
    PUBLISHED,
    HAPPENING,
    COMPLETED,
    CANCELLED,
    ;

    companion object {
        fun fromString(value: String?): EventStatus = entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: PUBLISHED
    }
}
