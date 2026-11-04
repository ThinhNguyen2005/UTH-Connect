package com.giathinh.uthconnect.presentation.attendance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.giathinh.uthconnect.domain.model.ServerErrorCode
import com.giathinh.uthconnect.domain.usecase.GetAttendanceRosterUseCase
import com.giathinh.uthconnect.domain.usecase.AttendanceCsvLabels
import com.giathinh.uthconnect.domain.usecase.ExportAttendanceCsvUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class AttendanceViewModel @Inject constructor(
    private val getRoster: GetAttendanceRosterUseCase,
    private val exportCsv: ExportAttendanceCsvUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AttendanceUiState())
    val uiState: StateFlow<AttendanceUiState> = _uiState.asStateFlow()
    private var eventId: String? = null
    private var loadJob: Job? = null

    fun load(eventId: String) {
        this.eventId = eventId
        loadJob?.cancel()
        _uiState.update { it.copy(loading = true, failure = null, errorCode = null, errorDetail = null) }
        loadJob = viewModelScope.launch {
            try {
                val roster = getRoster(eventId).getOrThrow()
                _uiState.update {
                    it.copy(
                        loading = false,
                        roster = roster,
                        failure = null,
                        errorCode = null,
                        errorDetail = null,
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(
                        loading = false,
                        roster = null,
                        failure = AttendanceFailure.LoadFailed,
                        errorCode = ServerErrorCode.of(error),
                        errorDetail = error.message,
                    )
                }
            }
        }
    }

    fun onAction(action: AttendanceAction) {
        when (action) {
            AttendanceAction.Refresh -> eventId?.let(::load)
            is AttendanceAction.Search -> _uiState.update { it.copy(query = action.query) }
            is AttendanceAction.Filter -> _uiState.update { it.copy(filter = action.filter) }
        }
    }

    suspend fun createExport(labels: AttendanceCsvLabels, exportedAt: Instant): Result<String> {
        val id = eventId ?: return Result.failure(IllegalStateException("Event not loaded"))
        return getRoster(id).map { roster -> exportCsv(roster, labels, exportedAt) }
    }

    /** Route báo một lần ghi file thất bại; không dựng chuỗi UI ở tầng này. */
    fun showExportFailure(reason: Throwable? = null) {
        _uiState.update {
            it.copy(
                failure = AttendanceFailure.ExportFailed,
                errorCode = reason?.let(ServerErrorCode::of),
                errorDetail = reason?.message,
            )
        }
    }
}
