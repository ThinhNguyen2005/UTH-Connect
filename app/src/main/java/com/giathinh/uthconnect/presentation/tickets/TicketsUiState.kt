package com.giathinh.uthconnect.presentation.tickets

import androidx.compose.runtime.Immutable
import com.giathinh.uthconnect.domain.model.Ticket

/** Dữ liệu cho danh sách vé và vé đang mở. */
@Immutable
data class TicketsUiState(
    val isLoading: Boolean = true,
    val tickets: List<Ticket> = emptyList(),
    val failure: TicketsFailure? = null,
    val selectedTicket: Ticket? = null,
    val secondsRemaining: Int = 60,
) {
    val isEmpty: Boolean get() = !isLoading && failure == null && tickets.isEmpty()
}

/** Lỗi tải danh sách vé. */
enum class TicketsFailure { LoadFailed }
