package com.giathinh.uthconnect.presentation.tickets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.giathinh.uthconnect.BuildConfig
import com.giathinh.uthconnect.domain.repository.TicketRepository
import com.giathinh.uthconnect.domain.usecase.GetProfileUseCase
import com.giathinh.uthconnect.domain.usecase.GetTicketsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.currentCoroutineContext
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
    private val ticketRepository: TicketRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(TicketsUiState())
    val uiState: StateFlow<TicketsUiState> = _uiState.asStateFlow()
    private val _events = Channel<TicketsEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()
    private var currentStudentId: String? = null
    private var qrJob: Job? = null

    init {
        viewModelScope.launch {
            getProfile.observe().collect { profile ->
                if (profile != null) {
                    currentStudentId = profile.studentId
                    _uiState.update { it.copy(studentName = profile.displayName) }
                    loadTickets(profile.studentId)
                } else {
                    qrJob?.cancel()
                    currentStudentId = null
                    _uiState.value = TicketsUiState(isLoading = false)
                }
            }
        }
    }

    fun onAction(action: TicketsAction) {
        when (action) {
            TicketsAction.Retry -> currentStudentId?.let(::loadTickets)
            is TicketsAction.SelectFilter -> _uiState.update { it.copy(filter = action.filter) }
            is TicketsAction.OpenTicket -> openTicket(action.ticketId)
            TicketsAction.CloseTicket -> {
                qrJob?.cancel()
                _uiState.update { it.copy(selectedTicket = null, qrToken = null, qrError = null) }
            }
        }
    }

    private fun loadTickets(studentId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, failure = null) }
            getTickets(studentId).fold(
                onSuccess = { tickets ->
                    if (currentStudentId == studentId) {
                        _uiState.update { it.copy(isLoading = false, tickets = tickets, failure = null) }
                    }
                },
                onFailure = {
                    _uiState.update { it.copy(isLoading = false, failure = TicketsFailure.LoadFailed) }
                },
            )
        }
    }

    private fun openTicket(ticketId: String) {
        val ticket = _uiState.value.tickets.firstOrNull { it.id == ticketId } ?: return
        qrJob?.cancel()
        _uiState.update { it.copy(
            selectedTicket = ticket, secondsRemaining = if (BuildConfig.USE_SUPABASE) 0 else QR_TTL_SECONDS,
            qrToken = null, qrError = null,
        ) }
        viewModelScope.launch { _events.send(TicketsEvent.OpenQr(ticketId)) }
        qrJob = viewModelScope.launch {
            if (BuildConfig.USE_SUPABASE) {
                refreshLiveQr(ticket)
                return@launch
            }
            while (_uiState.value.selectedTicket?.id == ticketId && _uiState.value.secondsRemaining > 0) {
                delay(ONE_SECOND_MILLIS)
                _uiState.update { state ->
                    if (state.selectedTicket?.id == ticketId) state.copy(secondsRemaining = state.secondsRemaining - 1) else state
                }
            }
        }
    }

    private suspend fun refreshLiveQr(ticket: com.giathinh.uthconnect.domain.model.Ticket) {
        while (_uiState.value.selectedTicket?.id == ticket.id) {
            if (ticket.status != com.giathinh.uthconnect.domain.model.TicketStatus.VALID) {
                _uiState.update { it.copy(qrToken = null, qrError = "Vé không còn hiệu lực") }
                return
            }
            val response = ticketRepository.issueCheckInToken(ticket.eventId)
            if (response.isFailure) {
                _uiState.update { it.copy(qrToken = null, secondsRemaining = 0,
                    qrError = response.exceptionOrNull()?.message ?: "Không thể cấp mã QR" ) }
                delay(5_000L)
                continue
            }
            val token = response.getOrThrow()
            val expiry = token.split('.').getOrNull(2)?.toLongOrNull()?.times(1000L)
            if (expiry == null) {
                _uiState.update { it.copy(qrToken = null, secondsRemaining = 0, qrError = "Mã QR không hợp lệ") }
                delay(5_000L)
                continue
            }
            while (currentCoroutineContext().isActive && _uiState.value.selectedTicket?.id == ticket.id) {
                val left = ((expiry - System.currentTimeMillis()) / 1_000L).toInt().coerceAtLeast(0)
                if (left <= 3) break
                _uiState.update { it.copy(qrToken = token, qrError = null, secondsRemaining = left) }
                delay(1_000L)
            }
            _uiState.update { it.copy(qrToken = null, secondsRemaining = 0) }
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
