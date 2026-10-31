package com.giathinh.uthconnect.presentation.tickets

import androidx.compose.runtime.Immutable
import com.giathinh.uthconnect.domain.model.Ticket
import com.giathinh.uthconnect.domain.model.TicketStatus

/** Dữ liệu cho danh sách vé, tab đang lọc và vé đang mở. */
@Immutable
data class TicketsUiState(
    val isLoading: Boolean = true,
    val tickets: List<Ticket> = emptyList(),
    val failure: TicketsFailure? = null,
    val selectedTicket: Ticket? = null,
    val secondsRemaining: Int = 60,
    val filter: TicketFilter = TicketFilter.Upcoming,
    /** Tên hiển thị của sinh viên đang đăng nhập — màn chi tiết vé in kèm MSSV. */
    val studentName: String? = null,
) {
    /** Vé còn hiệu lực — nhóm "Sắp diễn ra". */
    val upcomingTickets: List<Ticket> get() = tickets.filter { it.status == TicketStatus.VALID }

    /** Vé đã dùng hoặc đã hủy — nhóm "Đã qua". */
    val pastTickets: List<Ticket> get() = tickets.filter { it.status != TicketStatus.VALID }

    /** Danh sách khớp tab đang chọn; Screen chỉ render đúng danh sách này. */
    val visibleTickets: List<Ticket> get() = if (filter == TicketFilter.Upcoming) upcomingTickets else pastTickets

    val isEmpty: Boolean get() = !isLoading && failure == null && tickets.isEmpty()
}

/** Hai nhóm vé trên màn hình; nhóm suy ra từ [TicketStatus], không phải trường lưu trữ. */
enum class TicketFilter { Upcoming, Past }

/** Lỗi tải danh sách vé. */
enum class TicketsFailure { LoadFailed }
