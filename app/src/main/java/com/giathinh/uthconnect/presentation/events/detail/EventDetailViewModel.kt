package com.giathinh.uthconnect.presentation.events.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.giathinh.uthconnect.domain.usecase.GetEventsUseCase
import com.giathinh.uthconnect.domain.usecase.RegisterEventUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class EventDetailViewModel @Inject constructor(
    private val getEvents: GetEventsUseCase,
    private val registerEvent: RegisterEventUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(EventDetailUiState())
    val uiState = _uiState.asStateFlow()
    private var loadJob: Job? = null
    private var requestedEventId: String? = null

    fun load(eventId: String) {
        requestedEventId = eventId
        loadJob?.cancel()
        _uiState.update { it.copy(isLoading = true, failure = null) }
        loadJob = viewModelScope.launch {
            getEvents().fold(
                onSuccess = { events ->
                    val event = events.firstOrNull { it.id == eventId }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            event = event,
                            failure = if (event == null) EventDetailFailure.EventNotFound else null,
                        )
                    }
                },
                onFailure = {
                    _uiState.update { it.copy(isLoading = false, failure = EventDetailFailure.LoadFailed) }
                },
            )
        }
    }

    fun onAction(action: EventDetailAction) {
        when (action) {
            EventDetailAction.Retry -> requestedEventId?.let(::load)
            EventDetailAction.Register -> register()
        }
    }

    private fun register() {
        val event = _uiState.value.event ?: return
        if (!event.canRegister || _uiState.value.isRegistering || _uiState.value.registration != null) return

        _uiState.update { it.copy(isRegistering = true, registrationFailed = false) }
        viewModelScope.launch {
            registerEvent(event.id).fold(
                onSuccess = { registration ->
                    _uiState.update { it.copy(isRegistering = false, registration = registration) }
                },
                onFailure = {
                    _uiState.update { it.copy(isRegistering = false, registrationFailed = true) }
                },
            )
        }
    }
}
