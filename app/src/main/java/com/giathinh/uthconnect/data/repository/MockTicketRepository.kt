package com.giathinh.uthconnect.data.repository

import com.giathinh.uthconnect.domain.model.Attendance
import com.giathinh.uthconnect.domain.model.Ticket
import com.giathinh.uthconnect.domain.model.TicketStatus
import com.giathinh.uthconnect.domain.repository.TicketCheckInOutcome
import com.giathinh.uthconnect.domain.repository.TicketRepository
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/** Dữ liệu trình diễn cho CHECKIN-1; thay bằng nguồn sự kiện/đăng ký khi hợp đồng sẵn sàng. */
@Singleton
class MockTicketRepository @Inject constructor() : TicketRepository {
    private val attendances = mutableMapOf<String, Attendance>()

    override suspend fun getTickets(studentId: String): Result<List<Ticket>> = Result.success(
        synchronized(attendances) {
            mockTickets.filter { it.studentId == studentId }.map { ticket ->
                if (ticket.id in attendances) ticket.copy(status = TicketStatus.CHECKED_IN) else ticket
            }
        },
    )

    override suspend fun checkIn(identifier: String, organizerId: String): Result<TicketCheckInOutcome> = Result.success(
        synchronized(attendances) {
            val ticket = mockTickets.firstOrNull {
                it.id == identifier || it.fallbackCode.filterNot(Char::isWhitespace)
                    .equals(identifier.filterNot(Char::isWhitespace), ignoreCase = true)
            }
            when {
                ticket == null || ticket.status == TicketStatus.CANCELLED -> TicketCheckInOutcome.NOT_REGISTERED

                ticket.status == TicketStatus.CHECKED_IN || ticket.id in attendances ->
                    TicketCheckInOutcome.ALREADY_CHECKED_IN

                else -> {
                    attendances[ticket.id] = Attendance(
                        ticketId = ticket.id,
                        eventId = ticket.eventId,
                        studentId = ticket.studentId,
                        checkedInAt = Instant.now().toString(),
                        checkedInBy = organizerId,
                    )
                    TicketCheckInOutcome.SUCCESS
                }
            }
        },
    )

    private companion object {
        val mockTickets = listOf(
            Ticket(
                id = "ticket-sports",
                studentId = DEMO_STUDENT_ID,
                eventId = "event-sports",
                eventTitle = "Hội thao Sinh viên UTH 2026",
                eventLocation = "Sân vận động Cơ sở 2",
                eventStartsAt = "12/11/2026 · 07:00",
                status = TicketStatus.VALID,
                fallbackCode = "482 917",
                organizerName = "CLB Thể Thao UTH",
                seatLabel = "Ghế A-14",
            ),
            Ticket(
                id = "ticket-club",
                studentId = DEMO_STUDENT_ID,
                eventId = "event-club",
                eventTitle = "Giao lưu CLB Ánh Sáng",
                eventLocation = "Hội trường A.102",
                eventStartsAt = "15/11/2026 · 18:30",
                status = TicketStatus.CHECKED_IN,
                fallbackCode = "105 638",
                organizerName = "Đội CTXH Ánh Sáng",
                seatLabel = "Nhóm tự do",
            ),
        )
        const val DEMO_STUDENT_ID = "2023040123"
    }
}
