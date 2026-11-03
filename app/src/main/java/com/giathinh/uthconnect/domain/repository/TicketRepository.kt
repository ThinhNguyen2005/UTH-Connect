package com.giathinh.uthconnect.domain.repository

import com.giathinh.uthconnect.domain.model.Ticket

/** Đọc các vé hiện có của sinh viên. */
interface TicketRepository {
    suspend fun getTickets(studentId: String): Result<List<Ticket>>
    suspend fun checkIn(identifier: String, organizerId: String): Result<TicketCheckInOutcome>
}

enum class TicketCheckInOutcome { SUCCESS, ALREADY_CHECKED_IN, NOT_REGISTERED }
