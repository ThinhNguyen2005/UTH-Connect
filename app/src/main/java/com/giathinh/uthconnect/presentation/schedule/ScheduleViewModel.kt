package com.giathinh.uthconnect.presentation.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.giathinh.uthconnect.domain.model.EventStatus
import com.giathinh.uthconnect.domain.repository.EventRepository
import com.giathinh.uthconnect.domain.usecase.TimetableUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class ScheduleViewModel @Inject constructor(
    private val useCase: TimetableUseCase,
    private val eventsRepository: EventRepository,
    @param:Named("timetable") private val clock: Clock,
) : ViewModel() {
    private val initialNow = LocalDateTime.now(clock)
    private val state = MutableStateFlow(ScheduleUiState(now = initialNow, selectedDate = initialNow.toLocalDate()))
    val uiState = state.asStateFlow()
    private var eventsJob: Job? = null

    init {
        viewModelScope.launch {
            useCase.timetable.collect { value ->
                state.update { it.copy(timetable = value, isLoading = false) }
            }
        }
        loadEvents()
        viewModelScope.launch {
            while (true) {
                delay(60_000)
                state.update { it.copy(now = LocalDateTime.now(clock)) }
            }
        }
    }

    private fun loadEvents() {
        eventsJob?.cancel()
        state.update { it.copy(isLoadingEvents = true, eventError = null) }
        eventsJob = viewModelScope.launch {
            combine(eventsRepository.getEvents(), eventsRepository.currentUserRegistrations()) { events, registeredIds ->
                events.filter { it.status != EventStatus.DRAFT && it.status != EventStatus.CANCELLED }
                    .map { event ->
                        val start = LocalDateTime.ofInstant(Instant.ofEpochMilli(event.startAt), clock.zone)
                        val end = LocalDateTime.ofInstant(Instant.ofEpochMilli(event.endAt), clock.zone)
                        ScheduleEvent(
                            id = event.id,
                            title = event.title,
                            organizer = event.orgName,
                            location = event.location,
                            startAt = start,
                            endAt = end,
                            registration = if (event.id in registeredIds) EventRegistration.REGISTERED else EventRegistration.NOT_REGISTERED,
                            accent = if (event.id in registeredIds) EventAccent.PRIMARY else EventAccent.WARNING,
                            session = when (SchedulePeriod.fromHour(start.hour)) {
                                SchedulePeriod.MORNING -> "Morning"
                                SchedulePeriod.AFTERNOON -> "Afternoon"
                                else -> "Evening"
                            },
                        )
                    }
            }.catch { error ->
                if (error is CancellationException) throw error
                state.update { it.copy(isLoadingEvents = false, eventError = "Không thể tải sự kiện. Vui lòng thử lại.") }
            }.collect { events ->
                state.update { it.copy(events = events, isLoadingEvents = false, eventError = null) }
            }
        }
    }

    fun onAction(action: ScheduleAction) {
        when (action) {
            is ScheduleAction.SelectDate -> state.update { it.copy(selectedDate = action.value) }

            ScheduleAction.JumpToToday -> state.update { it.copy(selectedDate = it.now.toLocalDate()) }

            is ScheduleAction.SelectPeriod -> state.update { it.copy(period = action.value) }

            is ScheduleAction.SelectTab -> state.update { it.copy(tab = action.value) }

            ScheduleAction.RetryEvents -> loadEvents()

            is ScheduleAction.RegisterEvent -> {
                if (state.value.registeringEventId != null) return
                state.update { it.copy(registeringEventId = action.id, eventError = null) }
                viewModelScope.launch {
                    try {
                        eventsRepository.registerCurrentUser(action.id).getOrThrow()
                        eventsRepository.refresh()
                    } catch (error: Exception) {
                        if (error is CancellationException) throw error
                        state.update { it.copy(eventError = "Đăng ký thất bại. Kiểm tra kết nối hoặc điều kiện tham gia.") }
                    } finally {
                        state.update { it.copy(registeringEventId = null) }
                    }
                }
            }

            ScheduleAction.OpenForm -> state.update {
                it.copy(showForm = true, date = it.now.toLocalDate().plusDays(1).toString(), titleError = false, dateError = false, timeError = false)
            }

            ScheduleAction.CloseForm -> state.update {
                it.copy(showForm = false, titleError = false, dateError = false, timeError = false)
            }

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
