package com.giathinh.uthconnect.presentation.events

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.giathinh.uthconnect.presentation.common.messageText
import com.giathinh.uthconnect.presentation.scanner.QrScannerDialog

@Composable
fun EventsRoute(
    onOpenAttendance: (String) -> Unit,
    viewModel: EventsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var scannerOpen by remember { mutableStateOf(false) }
    EventsScreen(
        state = state,
        onRefresh = viewModel::reload,
        onRegister = viewModel::register,
        onCancel = viewModel::cancel,
        onSave = viewModel::saveEvent,
        onSetStatus = viewModel::setStatus,
        onOpenAttendance = onOpenAttendance,
        onScan = {
            // Bỏ tin nhắn của lượt đăng ký/lưu trước đó, nếu không máy quét sẽ tưởng
            // rằng vừa có một lượt điểm danh trả kết quả.
            viewModel.clearCheckInResult()
            scannerOpen = true
        },
    )
    if (scannerOpen) {
        QrScannerDialog(
            busy = state.checkingIn,
            resultMessage = state.message,
            errorMessage = state.errorCode?.messageText(state.error) ?: state.error,
            checkedInCount = state.checkedInCount,
            onDismiss = {
                scannerOpen = false
                viewModel.clearCheckInResult()
            },
            onContinue = viewModel::clearCheckInResult,
            onToken = viewModel::checkIn,
        )
    }
}
