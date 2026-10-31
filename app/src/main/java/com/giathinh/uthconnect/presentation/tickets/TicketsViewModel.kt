package com.giathinh.uthconnect.presentation.tickets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.giathinh.uthconnect.domain.usecase.GetProfileUseCase
import com.giathinh.uthconnect.domain.usecase.GetTicketsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect

/** Tải vé, xử lý chọn vé và giữ nhịp đếm ngược cho QR placeholder. */
@HiltViewModel
class TicketsViewModel @Inject constructor(
    private val getTickets: GetTicketsUseCase,
    private val getProfile: GetProfileUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(TicketsUiState())
    val uiState: StateFlow<TicketsUiState> = _uiState.asStateFlow()
    private val _events = Channel<TicketsEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()
    private var currentStudentId: String? = null

    init {
        viewModelScope.launch {
            getProfile.observe().collect { profile ->
                if (profile != null) {
                    currentStudentId = profile.studentId
                    loadTickets(profile.studentId)
                }
            }
        }
    }

    fun onAction(action: TicketsAction) {
        when (action) {
            TicketsAction.Retry -> currentStudentId?.let(::loadTickets)
            is TicketsAction.OpenTicket -> openTicket(action.ticketId)
            TicketsAction.CloseTicket -> _uiState.update { it.copy(selectedTicket = null) }
        }
    }

    private fun loadTickets(studentId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, failure = null) }
            getTickets(studentId).fold(
                onSuccess = { tickets ->
                    _uiState.update { it.copy(isLoading = false, tickets = tickets, failure = null) }
                },
                onFailure = {
                    _uiState.update { it.copy(isLoading = false, failure = TicketsFailure.LoadFailed) }
                },
            )
        }
    }

    private fun openTicket(ticketId: String) {
        val ticket = _uiState.value.tickets.firstOrNull { it.id == ticketId } ?: return
        _uiState.update { it.copy(selectedTicket = ticket, secondsRemaining = QR_TTL_SECONDS) }
        viewModelScope.launch { _events.send(TicketsEvent.OpenQr(ticketId)) }
        viewModelScope.launch {
            while (_uiState.value.selectedTicket?.id == ticketId && _uiState.value.secondsRemaining > 0) {
                delay(ONE_SECOND_MILLIS)
                _uiState.update { state ->
                    if (state.selectedTicket?.id == ticketId) state.copy(secondsRemaining = state.secondsRemaining - 1) else state
                }
            }
        }
    }

    private companion object {
        const val QR_TTL_SECONDS = 60
        const val ONE_SECOND_MILLIS = 1_000L
    }
}

/** One-shot navigation request emitted after selecting a valid ticket. */
sealed interface TicketsEvent {
    data class OpenQr(val ticketId: String) : TicketsEvent
}
