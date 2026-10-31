package com.giathinh.uthconnect.presentation.tickets

import androidx.compose.runtime.Composable
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

/** Route cấp tab của danh sách vé. */
@Serializable data object TicketsDestination

/** Vé được chọn; id là khóa tra cứu trong ViewModel cùng graph. */
@Serializable data class TicketQrDestination(val ticketId: String)

/** Graph con để danh sách và QR dùng cùng state holder trong scope của tab vé. */
fun NavGraphBuilder.ticketsGraph(navController: NavHostController) {
    composable<TicketsDestination> { TicketsRoute(navController) }
    composable<TicketQrDestination> { entry -> TicketQrRoute(entry, navController) }
}
