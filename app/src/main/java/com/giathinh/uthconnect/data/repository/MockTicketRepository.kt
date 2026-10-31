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
            Ticket("ticket-sports", DEMO_STUDENT_ID, "event-sports", "Hội thao Sinh viên UTH 2026", "Cơ sở Quang Trung", "12/11/2026 · 07:00", TicketStatus.VALID, "482 917"),
            Ticket("ticket-club", DEMO_STUDENT_ID, "event-club", "Giao lưu CLB Ánh Sáng", "Cơ sở Nguyễn Tất Thành", "15/11/2026 · 18:30", TicketStatus.CHECKED_IN, "105 638"),
        )
        const val DEMO_STUDENT_ID = "2023040123"
    }
}
