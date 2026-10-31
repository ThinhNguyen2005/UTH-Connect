package com.giathinh.uthconnect.domain.model

/** Vé đăng ký của một sinh viên cho một sự kiện. */
data class Ticket(
    val id: String,
    val studentId: String,
    val eventId: String,
    val eventTitle: String,
    val eventLocation: String,
    val eventStartsAt: String,
    val status: TicketStatus,
    val fallbackCode: String,
)

/** Trạng thái nghiệp vụ của vé. */
enum class TicketStatus { VALID, CHECKED_IN, CANCELLED }
