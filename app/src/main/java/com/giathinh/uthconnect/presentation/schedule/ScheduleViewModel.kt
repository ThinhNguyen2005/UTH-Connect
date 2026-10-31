package com.giathinh.uthconnect.presentation.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.giathinh.uthconnect.domain.usecase.TimetableUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.LocalDateTime
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
        viewModelScope.launch { useCase.timetable.collect { value -> state.update { it.copy(timetable = value) } } }
        viewModelScope.launch {
            while (true) {
                state.update { it.copy(now = LocalDateTime.now(clock)) }
                delay(60_000)
            }
        }
    }
    fun onAction(action: ScheduleAction) {
        when (action) {
            ScheduleAction.OpenForm -> state.update { it.copy(showForm = true, date = it.now.toLocalDate().plusDays(1).toString(), invalidForm = false) }
            ScheduleAction.CloseForm -> state.update { it.copy(showForm = false, invalidForm = false) }
            is ScheduleAction.Title -> state.update { it.copy(title = action.value, invalidForm = false) }
            is ScheduleAction.Date -> state.update { it.copy(date = action.value, invalidForm = false) }
            is ScheduleAction.Time -> state.update { it.copy(time = action.value, invalidForm = false) }
            is ScheduleAction.SelectCourse -> state.update { it.copy(course = action.value) }
            is ScheduleAction.Complete -> useCase.complete(action.id)
            ScheduleAction.Save -> {
                val value = state.value
                val valid = useCase.add(value.course, value.title, value.date, value.time)
                state.update { it.copy(showForm = !valid, invalidForm = !valid, title = if (valid) "" else it.title) }
            }
        }
    }
}
