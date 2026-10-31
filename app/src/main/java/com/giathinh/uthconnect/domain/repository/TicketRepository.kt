package com.giathinh.uthconnect.domain.repository

import com.giathinh.uthconnect.domain.model.Ticket

/** Đọc các vé hiện có của sinh viên. */
interface TicketRepository {
    suspend fun getTickets(studentId: String): Result<List<Ticket>>
}
