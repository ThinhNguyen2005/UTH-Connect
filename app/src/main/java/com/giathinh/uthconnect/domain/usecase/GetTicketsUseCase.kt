package com.giathinh.uthconnect.domain.usecase

import com.giathinh.uthconnect.domain.model.Ticket
import com.giathinh.uthconnect.domain.repository.TicketRepository
import javax.inject.Inject

/** Lấy danh sách vé của sinh viên hiện tại. */
class GetTicketsUseCase @Inject constructor(private val repository: TicketRepository) {
    suspend operator fun invoke(studentId: String): Result<List<Ticket>> = repository.getTickets(studentId)
}
