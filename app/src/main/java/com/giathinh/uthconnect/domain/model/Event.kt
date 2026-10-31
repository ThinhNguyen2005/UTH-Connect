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
    val status: EventStatus = EventStatus.PUBLISHED,
    val coverUrl: String? = null,
)

enum class EventStatus {
    DRAFT,
    PUBLISHED,
    HAPPENING,
    COMPLETED,
    CANCELLED;

    companion object {
        fun fromString(value: String?): EventStatus {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: PUBLISHED
        }
    }
}
