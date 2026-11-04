package com.giathinh.uthconnect.domain.repository

import com.giathinh.uthconnect.domain.model.Ticket

/** Đọc các vé hiện có của sinh viên. */
interface TicketRepository {
    suspend fun getTickets(studentId: String): Result<List<Ticket>>
    suspend fun issueCheckInToken(eventId: String): Result<String> =
        Result.failure(UnsupportedOperationException("QR server unavailable"))
    suspend fun checkIn(token: String): Result<Unit> =
        Result.failure(UnsupportedOperationException("QR check-in unavailable"))
    suspend fun checkIn(identifier: String, organizerId: String): Result<TicketCheckInOutcome> =
        Result.failure(UnsupportedOperationException("QR check-in unavailable"))
}

enum class TicketCheckInOutcome { SUCCESS, ALREADY_CHECKED_IN, NOT_REGISTERED }
