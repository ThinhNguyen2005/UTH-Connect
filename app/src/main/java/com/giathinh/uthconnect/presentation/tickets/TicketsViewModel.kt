package com.giathinh.uthconnect.presentation.tickets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.giathinh.uthconnect.BuildConfig
import com.giathinh.uthconnect.domain.model.ServerErrorCode
import com.giathinh.uthconnect.domain.repository.TicketRepository
import com.giathinh.uthconnect.domain.usecase.GetProfileUseCase
import com.giathinh.uthconnect.domain.model.UserRole
import com.giathinh.uthconnect.domain.model.TicketQrPayload
import com.giathinh.uthconnect.domain.usecase.GetTicketsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect

/** Tải vé, xử lý chọn vé và cấp mã QR vé ổn định từ backend. */
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
                    _uiState.update { it.copy(studentName = profile.displayName, isOrganizer = profile.role == UserRole.ORGANIZER) }
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
            TicketsAction.OpenScanner -> if (_uiState.value.isOrganizer) viewModelScope.launch { _events.send(TicketsEvent.OpenScanner) }
            TicketsAction.Retry -> currentStudentId?.let(::loadTickets)
            is TicketsAction.SelectFilter -> _uiState.update { it.copy(filter = action.filter) }
            is TicketsAction.OpenTicket -> openTicket(action.ticketId)
            TicketsAction.CloseTicket -> {
                qrJob?.cancel()
                _uiState.update {
                    it.copy(selectedTicket = null, qrToken = null, qrError = null, qrErrorCode = null)
                }
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
            qrToken = null, qrError = null, qrErrorCode = null,
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
                    if (state.selectedTicket?.id != ticketId) state
                    else if (state.secondsRemaining <= 1) {
                        state.copy(
                            secondsRemaining = QR_TTL_SECONDS,
                            qrPayload = TicketQrPayload.create(ticketId, System.currentTimeMillis()),
                        )
                    } else state.copy(secondsRemaining = state.secondsRemaining - 1)
                }
            }
        }
    }

    private suspend fun refreshLiveQr(ticket: com.giathinh.uthconnect.domain.model.Ticket) {
        if (ticket.status != com.giathinh.uthconnect.domain.model.TicketStatus.VALID) {
            _uiState.update {
                it.copy(qrToken = null, qrError = null, qrErrorCode = ServerErrorCode.TicketUnavailable)
            }
            return
        }
        ticketRepository.issueCheckInToken(ticket.eventId).fold(
            onSuccess = { token ->
                _uiState.update {
                    it.copy(qrToken = token, qrError = null, qrErrorCode = null, secondsRemaining = 0)
                }
            },
            onFailure = { error ->
                _uiState.update {
                    it.copy(qrToken = null, qrError = error.message, qrErrorCode = ServerErrorCode.of(error))
                }
            },
        )
    }

    private companion object {
        const val QR_TTL_SECONDS = 60
        const val ONE_SECOND_MILLIS = 1_000L
    }
}

/** One-shot navigation request emitted after selecting a valid ticket. */
sealed interface TicketsEvent {
    data object OpenScanner : TicketsEvent
    data class OpenQr(val ticketId: String) : TicketsEvent
}
