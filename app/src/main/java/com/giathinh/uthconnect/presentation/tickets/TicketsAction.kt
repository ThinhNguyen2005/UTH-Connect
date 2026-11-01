package com.giathinh.uthconnect.presentation.tickets

/** Ý định từ màn vé gửi tới ViewModel. */
sealed interface TicketsAction {
    data object Retry : TicketsAction
    data class SelectFilter(val filter: TicketFilter) : TicketsAction
    data class OpenTicket(val ticketId: String) : TicketsAction
    data object CloseTicket : TicketsAction
}
