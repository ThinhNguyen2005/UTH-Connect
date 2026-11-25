package com.giathinh.uthconnect.presentation.events

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.giathinh.uthconnect.R
import com.giathinh.uthconnect.presentation.common.messageText
import com.giathinh.uthconnect.presentation.scanner.QrScannerDialog

@Composable
fun EventsRoute(
    onOpenAttendance: (String) -> Unit,
    focusedEventId: String? = null,
    onNavigateBack: () -> Unit = {},
    viewModel: EventsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var scannerOpen by remember { mutableStateOf(false) }
    EventsScreen(
        state = state,
        checkInResultMessage = if (!scannerOpen) {
            state.checkInAlreadyAt?.let { stringResource(R.string.event_checkin_already_at, it) }
                ?: if (state.checkInSucceeded) stringResource(R.string.event_checkin_success) else null
        } else {
            null
        },
        checkInErrorMessage = if (!scannerOpen) {
            state.checkInErrorCode?.messageText(state.checkInError) ?: state.checkInError
        } else {
            null
        },
        onRefresh = viewModel::reload,
        onRegister = viewModel::register,
        onCancel = viewModel::cancel,
        onSave = viewModel::saveEvent,
        onSetStatus = viewModel::setStatus,
        onOpenAttendance = onOpenAttendance,
        focusedEventId = focusedEventId,
        onNavigateBack = onNavigateBack,
        onScan = {
            viewModel.clearCheckInResult()
            scannerOpen = true
        },
        onDiscussionAction = viewModel::onDiscussionAction,
    )
    if (scannerOpen) {
        QrScannerDialog(
            busy = state.checkingIn,
            resultMessage = if (state.checkInSucceeded) stringResource(R.string.event_checkin_success) else null,
            alreadyCheckedInMessage = state.checkInAlreadyAt?.let {
                stringResource(R.string.event_checkin_already_at, it)
            },
            errorMessage = state.checkInErrorCode?.messageText(state.checkInError) ?: state.checkInError,
            checkedInCount = state.checkedInCount,
            onDismiss = {
                scannerOpen = false
                if (!state.checkingIn) viewModel.clearCheckInResult()
            },
            onContinue = viewModel::clearCheckInResult,
            onToken = viewModel::checkIn,
        )
    }
}
