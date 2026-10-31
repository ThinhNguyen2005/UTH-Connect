package com.giathinh.uthconnect.presentation.events

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun EventsRoute(
    onOpenDetail: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EventsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    EventsScreen(
        uiState = uiState,
        onAction = { action ->
            when (action) {
                is EventsAction.OpenDetail -> onOpenDetail(action.eventId)
                is EventsAction.Register -> {
                    // Chưa đăng ký thật: chỉ mở detail nếu sự kiện vẫn cho phép đăng ký.
                    if (uiState.events.any { it.id == action.eventId && it.canRegister }) {
                        onOpenDetail(action.eventId)
                    }
                }
                else -> viewModel.onAction(action)
            }
        },
        onBack = onBack,
        modifier = modifier,
    )
}
