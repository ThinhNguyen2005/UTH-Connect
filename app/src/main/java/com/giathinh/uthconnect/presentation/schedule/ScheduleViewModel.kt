package com.giathinh.uthconnect.presentation.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.giathinh.uthconnect.domain.model.CourseSchedule
import com.giathinh.uthconnect.domain.model.Deadline
import com.giathinh.uthconnect.domain.usecase.TimetableUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class ScheduleViewModel @Inject constructor(
    private val timetable: TimetableUseCases,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ScheduleUiState())
    val uiState = _uiState.asStateFlow()
    private var observation: Job? = null

    init {
        observe()
    }

    fun onAction(action: ScheduleAction) {
        when (action) {
            ScheduleAction.Retry -> observe()
            ScheduleAction.RefreshReminders -> mutate { timetable.refreshReminders() }
            ScheduleAction.ShowClasses -> _uiState.update { it.copy(selectedTab = ScheduleTab.Classes) }
            ScheduleAction.ShowDeadlines -> _uiState.update { it.copy(selectedTab = ScheduleTab.Deadlines) }
            ScheduleAction.PreviousWeek -> _uiState.update {
                it.copy(selectedDate = it.selectedDate.minusWeeks(1))
            }
            ScheduleAction.NextWeek -> _uiState.update {
                it.copy(selectedDate = it.selectedDate.plusWeeks(1))
            }
            ScheduleAction.Today -> _uiState.update { it.copy(selectedDate = LocalDate.now()) }
            is ScheduleAction.SelectDate -> _uiState.update { it.copy(selectedDate = action.date) }
            is ScheduleAction.SetWeekView -> _uiState.update { it.copy(isWeekView = action.enabled) }
            is ScheduleAction.SaveSchedule -> mutate { timetable.saveSchedule(action.schedule) }
            is ScheduleAction.DeleteSchedule -> mutate { timetable.deleteSchedule(action.id) }
            is ScheduleAction.SaveDeadline -> mutate { timetable.saveDeadline(action.deadline) }
            is ScheduleAction.DeleteDeadline -> mutate { timetable.deleteDeadline(action.id) }
        }
    }

    private fun observe() {
        observation?.cancel()
        observation = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, failure = null) }
            combine(timetable.observeSchedules(), timetable.observeDeadlines()) { schedules, deadlines ->
                schedules to deadlines
            }.catch {
                _uiState.update { state -> state.copy(isLoading = false, failure = ScheduleFailure.LoadFailed) }
            }.collect { (schedules, deadlines) ->
                _uiState.update {
                    it.copy(isLoading = false, schedules = schedules, deadlines = deadlines, failure = null)
                }
            }
        }
    }

    private fun mutate(action: suspend () -> Result<Unit>) {
        viewModelScope.launch {
            action().fold(
                onSuccess = { _uiState.update { it.copy(failure = null) } },
                onFailure = { _uiState.update { it.copy(failure = ScheduleFailure.SaveFailed) } },
            )
        }
    }
}
