package com.giathinh.uthconnect.presentation.tickets

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Build
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.toRoute
import com.giathinh.uthconnect.presentation.scanner.ScannerDestination
import com.giathinh.uthconnect.R
import com.giathinh.uthconnect.presentation.common.messageText

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
                TicketsEvent.OpenScanner -> navController.navigate(ScannerDestination)
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
    val context = LocalContext.current
    TicketQrScreen(
        ticket = state.selectedTicket?.takeIf { it.id == ticketId },
        studentName = state.studentName,
        secondsRemaining = state.secondsRemaining,
        qrToken = state.qrToken,
        qrError = state.qrErrorCode?.messageText(state.qrError) ?: state.qrError,
        onCopy = { code ->
            context.getSystemService(ClipboardManager::class.java)
                ?.setPrimaryClip(ClipData.newPlainText(context.getString(R.string.tickets_detail_copy), code))
            // Android 13+ tự hiện xác nhận sao chép; máy cũ cần toast để vẫn có phản hồi.
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                val copied = context.getString(R.string.tickets_detail_copied, code)
                Toast.makeText(context, copied, Toast.LENGTH_SHORT).show()
            }
        },
        onBack = {
            viewModel.onAction(TicketsAction.CloseTicket)
            navController.popBackStack()
        },
    )
}
