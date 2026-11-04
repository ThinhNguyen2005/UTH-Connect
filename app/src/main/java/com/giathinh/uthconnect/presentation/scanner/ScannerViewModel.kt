package com.giathinh.uthconnect.presentation.scanner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.giathinh.uthconnect.domain.model.UserRole
import com.giathinh.uthconnect.domain.repository.SessionRepository
import com.giathinh.uthconnect.domain.usecase.CheckInResult
import com.giathinh.uthconnect.domain.usecase.CheckInUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class ScannerViewModel @Inject constructor(
    private val checkIn: CheckInUseCase,
    private val session: SessionRepository,
) : ViewModel() {
    private val state = MutableStateFlow(ScannerUiState())
    val uiState = state.asStateFlow()
    private val resultEvents = Channel<CheckInResult>(Channel.BUFFERED)
    val events = resultEvents.receiveAsFlow()

    init {
        viewModelScope.launch {
            session.session.collect { current ->
                val allowed = current.profile?.role == UserRole.ORGANIZER
                state.update { if (allowed) it.copy(authorized = true) else ScannerUiState() }
            }
        }
    }

    fun onAction(action: ScannerAction) {
        if (session.session.value.profile?.role != UserRole.ORGANIZER) return
        when (action) {
            is ScannerAction.CameraPermissionChanged -> state.update { it.copy(cameraPermissionGranted = action.granted, cameraError = false) }
            is ScannerAction.CameraErrorChanged -> state.update { it.copy(cameraError = action.hasError) }
            is ScannerAction.CameraCodeDetected -> {
                if (state.value.code.isBlank() && !state.value.isSubmitting) {
                    state.update { it.copy(code = action.value, result = null) }
                    submitCode(action.value)
                }
            }
            ScannerAction.ClearScan -> if (!state.value.isSubmitting) {
                state.update { it.copy(code = "", result = null, cameraError = false, scanSession = it.scanSession + 1) }
            }
            is ScannerAction.CodeChanged -> if (!state.value.isSubmitting) {
                state.update { it.copy(code = action.value, result = null) }
            }
            ScannerAction.Submit -> submitCode(state.value.code)
        }
    }

    private fun submitCode(code: String) {
        if (state.value.isSubmitting) return
        state.update { it.copy(isSubmitting = true, result = null) }
        viewModelScope.launch {
            val result = checkIn(code)
            state.update {
                it.copy(
                    result = result,
                    isSubmitting = false,
                    successCount = it.successCount + if (result == CheckInResult.SUCCESS) 1 else 0,
                )
            }
            resultEvents.send(result)
        }
    }
}
