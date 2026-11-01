package com.giathinh.uthconnect.presentation.tickets

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.toRoute

/** Nối state holder với màn danh sách và điều hướng đến vé đã chọn. */
@Composable
fun TicketsRoute(
    navController: NavHostController,
    viewModel: TicketsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is TicketsEvent.OpenQr -> navController.navigate(TicketQrDestination(event.ticketId))
            }
        }
    }
    TicketsScreen(
        uiState = state,
        onAction = viewModel::onAction,
    )
}

/** Màn QR dùng state holder của graph cha để tránh tải lại danh sách khi mở vé. */
@Composable
fun TicketQrRoute(
    entry: NavBackStackEntry,
    navController: NavHostController,
) {
    val parentEntry = remember(entry) { navController.getBackStackEntry(TicketsDestination) }
    val viewModel: TicketsViewModel = hiltViewModel(parentEntry)
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val ticketId = entry.toRoute<TicketQrDestination>().ticketId
    TicketQrScreen(
        ticket = state.selectedTicket?.takeIf { it.id == ticketId },
        studentName = state.studentName,
        secondsRemaining = state.secondsRemaining,
        onBack = {
            viewModel.onAction(TicketsAction.CloseTicket)
            navController.popBackStack()
        },
    )
}
