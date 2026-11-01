package com.giathinh.uthconnect.data.repository

import com.giathinh.uthconnect.domain.model.Ticket
import com.giathinh.uthconnect.domain.model.TicketStatus
import com.giathinh.uthconnect.domain.repository.TicketRepository
import javax.inject.Inject
import javax.inject.Singleton

/** Dữ liệu trình diễn cho CHECKIN-1; thay bằng nguồn sự kiện/đăng ký khi hợp đồng sẵn sàng. */
@Singleton
class MockTicketRepository @Inject constructor() : TicketRepository {
    override suspend fun getTickets(studentId: String): Result<List<Ticket>> =
        Result.success(mockTickets.filter { it.studentId == studentId })

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
