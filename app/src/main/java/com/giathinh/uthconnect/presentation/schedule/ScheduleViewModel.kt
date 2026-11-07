package com.giathinh.uthconnect.presentation.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.giathinh.uthconnect.BuildConfig
import com.giathinh.uthconnect.domain.model.Event
import com.giathinh.uthconnect.domain.repository.EventRepository
import com.giathinh.uthconnect.domain.repository.TicketRepository
import com.giathinh.uthconnect.domain.usecase.TimetableUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class ScheduleViewModel @Inject constructor(
    private val useCase: TimetableUseCase,
    private val eventRepository: EventRepository,
    private val ticketRepository: TicketRepository,
    @param:Named("timetable") private val clock: Clock,
) : ViewModel() {
    private var loadedEvents: List<Event> = emptyList()
    private var registeredEventIds: Set<String> = emptySet()
    private var ticketQrJob: Job? = null

    private val initialNow = LocalDateTime.now(clock)
    private val initialDate = initialNow.toLocalDate()

    private val state = MutableStateFlow(
        ScheduleUiState(
            now = initialNow,
            selectedDate = initialDate,
        ),
    )
    val uiState = state.asStateFlow()

    init {
        viewModelScope.launch {
            useCase.timetable.collect { value ->
                state.update { it.copy(timetable = value, isLoading = false) }
            }
        }
        viewModelScope.launch {
            eventRepository.getEvents().retryWhen { cause, _ ->
                if (cause is CancellationException) throw cause
                state.update { it.copy(isEventsLoading = false, eventsLoadFailed = true) }
                delay(60_000)
                true
            }.collect { events ->
                loadedEvents = events
                state.update {
                    it.copy(
                        events = events.mapNotNull(::toScheduleEvent),
                        isEventsLoading = false,
                        eventsLoadFailed = false,
                    )
                }
            }
        }
        viewModelScope.launch {
            eventRepository.currentUserRegistrations().catch { /* Events remain usable if registrations are unavailable. */ }
                .collect { ids ->
                    registeredEventIds = ids
                    state.update { current -> current.copy(events = loadedEvents.mapNotNull(::toScheduleEvent)) }
                }
        }
        viewModelScope.launch {
            while (true) {
                delay(60_000)
                val currentNow = LocalDateTime.now(clock)
                state.update { it.copy(now = currentNow) }
                eventRepository.refresh()
            }
        }
    }

    fun onAction(action: ScheduleAction) {
        when (action) {
            is ScheduleAction.SelectDate -> state.update {
                it.copy(selectedDate = action.value)
            }

            ScheduleAction.JumpToToday -> {
                val today = state.value.now.toLocalDate()
                state.update {
                    it.copy(selectedDate = today)
                }
            }

            is ScheduleAction.SelectPeriod -> state.update { it.copy(period = action.value) }

            is ScheduleAction.SelectTab -> state.update { it.copy(tab = action.value) }

            is ScheduleAction.RegisterEvent -> {
                val event = state.value.events.firstOrNull { it.id == action.id } ?: return
                if (LocalDateTime.now(clock) >= event.endAt) return
                viewModelScope.launch {
                    eventRepository.registerCurrentUser(action.id).onSuccess {
                        eventRepository.refresh()
                    }
                }
            }

            is ScheduleAction.OpenEventDetails -> {
                val event = state.value.events.firstOrNull { it.id == action.id } ?: return
                if (event.id !in registeredEventIds || !event.canOpenTicketAt(LocalDateTime.now(clock))) return
                ticketQrJob?.cancel()
                state.update {
                    it.copy(selectedEvent = event, ticketQrToken = null, ticketQrFailed = false)
                }
                if (BuildConfig.USE_SUPABASE) {
                    ticketQrJob = viewModelScope.launch {
                        ticketRepository.issueCheckInToken(action.id).fold(
                            onSuccess = { token ->
                                state.update { current ->
                                    if (current.selectedEvent?.id == action.id) {
                                        current.copy(ticketQrToken = token, ticketQrFailed = false)
                                    } else {
                                        current
                                    }
                                }
                            },
                            onFailure = {
                                state.update { current ->
                                    if (current.selectedEvent?.id == action.id) {
                                        current.copy(ticketQrToken = null, ticketQrFailed = true)
                                    } else {
                                        current
                                    }
                                }
                            },
                        )
                    }
                }
            }

            ScheduleAction.CloseEventDetails -> {
                ticketQrJob?.cancel()
                state.update {
                    it.copy(selectedEvent = null, ticketQrToken = null, ticketQrFailed = false)
                }
            }

            ScheduleAction.OpenForm -> state.update {
                it.copy(
                    showForm = true,
                    date = it.now.toLocalDate().plusDays(1).toString(),
                    titleError = false,
                    dateError = false,
                    timeError = false,
                )
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

    private fun toScheduleEvent(event: Event): ScheduleEvent? = runCatching {
        val startAt = Instant.ofEpochMilli(event.startAt).atZone(clock.zone ?: ZoneId.systemDefault()).toLocalDateTime()
        val endAt = Instant.ofEpochMilli(event.endAt).atZone(clock.zone ?: ZoneId.systemDefault()).toLocalDateTime()
        ScheduleEvent(
            id = event.id,
            title = event.title,
            organizer = event.orgName.ifBlank { event.orgId },
            location = event.location,
            startAt = startAt,
            endAt = endAt,
            registration = if (event.id in registeredEventIds) EventRegistration.REGISTERED else EventRegistration.NOT_REGISTERED,
            accent = if (event.id in registeredEventIds) EventAccent.PRIMARY else EventAccent.WARNING,
            status = event.status,
            session = when (startAt.hour) {
                in 0..11 -> "Morning"
                in 12..17 -> "Afternoon"
                else -> "Evening"
            },
        )
    }.getOrNull()
}
