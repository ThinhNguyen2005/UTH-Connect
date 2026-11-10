package com.giathinh.uthconnect.presentation.tickets

import androidx.compose.runtime.Composable
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

/**
 * Điểm vào là hàng "Vé của tôi" trong tab Cá nhân (`docs/Design.md` mục 8.2), không còn
 * là tab đáy — khung app giữ đúng 4 điểm đến theo khuyến nghị 3–5 của Google.
 */
@Serializable data object TicketsDestination

/** Vé được chọn; id là khóa tra cứu trong ViewModel cùng graph. */
@Serializable data class TicketQrDestination(val ticketId: String)

/** Graph con để danh sách và QR dùng cùng state holder trong scope của tab vé. */
fun NavGraphBuilder.ticketsGraph(navController: NavHostController) {
    composable<TicketsDestination> { TicketsRoute(navController) }
    composable<TicketQrDestination> { entry -> TicketQrRoute(entry, navController) }
}
