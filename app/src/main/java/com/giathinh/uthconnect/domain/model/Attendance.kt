package com.giathinh.uthconnect.domain.model

/** Bản ghi điểm danh gắn với vé; thời gian do backend cung cấp khi tích hợp thật. */
data class Attendance(
    val ticketId: String,
    val eventId: String,
    val studentId: String,
    val checkedInAt: String?,
    val checkedInBy: String?,
)
