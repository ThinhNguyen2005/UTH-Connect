package com.giathinh.uthconnect.domain.model

data class AttendanceEntry(
    val userId: String,
    val studentId: String,
    val displayName: String,
    val email: String,
    val checkedInAt: String?,
    val checkedInBy: String?,
) {
    val checkedIn: Boolean get() = checkedInAt != null
}

data class AttendanceRoster(
    val eventId: String,
    val eventTitle: String,
    val entries: List<AttendanceEntry>,
) {
    val registeredCount: Int get() = entries.size
    val checkedInCount: Int get() = entries.count { it.checkedIn }
    val notCheckedInCount: Int get() = registeredCount - checkedInCount
}
