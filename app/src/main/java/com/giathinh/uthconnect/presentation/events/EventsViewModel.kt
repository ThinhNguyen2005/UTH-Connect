package com.giathinh.uthconnect.presentation.events

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.giathinh.uthconnect.domain.model.Campus
import com.giathinh.uthconnect.domain.usecase.GetEventsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class EventsViewModel @Inject constructor(
    private val getEvents: GetEventsUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(EventsUiState())
    val uiState = _uiState.asStateFlow()
    private var loadJob: Job? = null

    init { load() }

    fun onAction(action: EventsAction) {
        when (action) {
            EventsAction.Retry -> load()
            is EventsAction.SelectCampus -> load(action.campus)
            is EventsAction.OpenDetail, is EventsAction.Register -> {
                // Route xử lý điều hướng, ViewModel chỉ giữ trạng thái danh sách.
            }
        }
    }

    private fun load(campus: Campus? = _uiState.value.selectedCampus) {
        // Khi bấm chip liên tục, hủy lần tải cũ để kết quả không đè lên bộ lọc mới.
        loadJob?.cancel()
        _uiState.update { it.copy(isLoading = true, selectedCampus = campus, failure = null) }
        loadJob = viewModelScope.launch {
            getEvents(campus).fold(
                onSuccess = { events ->
                    _uiState.update { it.copy(isLoading = false, events = events, failure = null) }
                },
                onFailure = {
                    _uiState.update {
                        it.copy(isLoading = false, events = emptyList(), failure = EventsFailure.LoadFailed)
                    }
                },
            )
        }
    }
}
