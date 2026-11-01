package com.giathinh.uthconnect.presentation.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.giathinh.uthconnect.domain.usecase.TimetableUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.LocalDateTime
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Named
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class ScheduleViewModel @Inject constructor(
    private val useCase: TimetableUseCase,
    @param:Named("timetable") private val clock: Clock,
) : ViewModel() {
    private val state = MutableStateFlow(ScheduleUiState(now = LocalDateTime.now(clock)))
    val uiState = state.asStateFlow()
    init {
        viewModelScope.launch {
            useCase.timetable.collect { value ->
                state.update { it.copy(timetable = value, isLoading = false) }
            }
        }
        viewModelScope.launch {
            while (true) {
                state.update { it.copy(now = LocalDateTime.now(clock)) }
                delay(60_000)
            }
        }
    }
    fun onAction(action: ScheduleAction) {
        when (action) {
            ScheduleAction.OpenForm -> state.update { it.copy(showForm = true, date = it.now.toLocalDate().plusDays(1).toString(), titleError = false, dateError = false, timeError = false) }
            ScheduleAction.CloseForm -> state.update { it.copy(showForm = false, titleError = false, dateError = false, timeError = false) }
            is ScheduleAction.Title -> state.update { it.copy(title = action.value, titleError = false) }
            is ScheduleAction.Date -> state.update { it.copy(date = action.value, dateError = false) }
            is ScheduleAction.Time -> state.update { it.copy(time = action.value, timeError = false) }
            is ScheduleAction.SelectCourse -> state.update { it.copy(course = action.value) }
            is ScheduleAction.Complete -> useCase.complete(action.id)
            ScheduleAction.Save -> {
                val value = state.value
                val titleError = value.title.isBlank()
                val dateError = runCatching { LocalDate.parse(value.date.trim()) }.isFailure
                val timeError = runCatching { LocalTime.parse(value.time.trim()) }.isFailure
                if (titleError || dateError || timeError) {
                    state.update { it.copy(titleError = titleError, dateError = dateError, timeError = timeError) }
                    return
                }
                val valid = useCase.add(value.course, value.title, value.date, value.time)
                state.update { it.copy(showForm = !valid, dateError = !valid, timeError = !valid, title = if (valid) "" else it.title) }
            }
        }
    }
}
