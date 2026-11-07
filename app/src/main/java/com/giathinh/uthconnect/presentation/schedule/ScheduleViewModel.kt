package com.giathinh.uthconnect.presentation.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.giathinh.uthconnect.domain.model.Event
import com.giathinh.uthconnect.domain.model.EventDraft
import com.giathinh.uthconnect.domain.model.EventStatus
import com.giathinh.uthconnect.domain.model.StudentProfile
import com.giathinh.uthconnect.domain.model.UserRole
import com.giathinh.uthconnect.domain.repository.EventRepository
import com.giathinh.uthconnect.domain.repository.SessionRepository
import com.giathinh.uthconnect.domain.repository.StudentSession
import com.giathinh.uthconnect.domain.usecase.GetProfileUseCase
import com.giathinh.uthconnect.domain.usecase.TimetableUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class ScheduleViewModel @Inject constructor(
    private val useCase: TimetableUseCase,
    @param:Named("timetable") private val clock: Clock,
    private val getProfile: GetProfileUseCase,
    private val eventRepository: EventRepository,
) : ViewModel() {

    constructor(
        useCase: TimetableUseCase,
        clock: Clock,
    ) : this(
        useCase = useCase,
        clock = clock,
        getProfile = GetProfileUseCase(object : SessionRepository {
            private val sessionState = MutableStateFlow(StudentSession(isRestoring = false))
            override val session: StateFlow<StudentSession> = sessionState.asStateFlow()
            override suspend fun restore() = Unit
            override suspend fun start(profile: StudentProfile, remember: Boolean) {
                sessionState.value = StudentSession(isRestoring = false, profile = profile)
            }
            override suspend fun updateProfile(profile: StudentProfile) {
                sessionState.value = sessionState.value.copy(profile = profile)
            }
            override suspend fun enableBiometric(studentId: String) = Unit
            override suspend fun disableBiometric() = Unit
            override suspend fun signOut() = Unit
        }),
        eventRepository = object : EventRepository {
            private val list = MutableStateFlow<List<Event>>(emptyList())
            private val registered = MutableStateFlow<Set<String>>(emptySet())
            override fun getEvents(campus: String?): Flow<List<Event>> = list
            override suspend fun getEventDetail(eventId: String): Result<Event?> = Result.success(list.value.firstOrNull { it.id == eventId })
            override suspend fun registerEvent(eventId: String, uid: String, studentId: String, studentName: String): Result<Unit> {
                registered.value = registered.value + eventId
                return Result.success(Unit)
            }
            override suspend fun cancelRegistration(eventId: String, uid: String): Result<Unit> {
                registered.value = registered.value - eventId
                return Result.success(Unit)
            }
            override fun isRegistered(eventId: String, uid: String): Flow<Boolean> = flowOf(registered.value.contains(eventId))
            override suspend fun registerCurrentUser(eventId: String): Result<Unit> {
                registered.value = registered.value + eventId
                return Result.success(Unit)
            }
            override suspend fun cancelCurrentRegistration(eventId: String): Result<Unit> {
                registered.value = registered.value - eventId
                return Result.success(Unit)
            }
            override fun currentUserRegistrations(): Flow<Set<String>> = registered
            override suspend fun saveEvent(eventId: String?, draft: EventDraft): Result<Unit> {
                val newEvent = Event(
                    id = eventId ?: "event-${System.currentTimeMillis()}",
                    title = draft.title,
                    description = draft.description,
                    orgId = "GV2023001",
                    orgName = "ThS. Trần Thu Trang",
                    location = draft.location,
                    campus = draft.campus,
                    startAt = draft.startAt,
                    endAt = draft.endAt,
                    capacity = draft.capacity,
                )
                list.value = list.value + newEvent
                return Result.success(Unit)
            }
        },
    )

    private val initialNow = LocalDateTime.now(clock)
    private val initialDate = initialNow.toLocalDate()

    private val registeredEventIds = mutableSetOf<String>()
    private var allStoredEvents = listOf<Event>()

    private val state = MutableStateFlow(
        ScheduleUiState(
            now = initialNow,
            selectedDate = initialDate,
            events = emptyList(),
        ),
    )
    val uiState = state.asStateFlow()

    init {
        viewModelScope.launch {
            getProfile.observe().collect { profile ->
                val isTeacher = profile?.role?.isTeacherOrOrganizer == true ||
                    profile?.role?.name in listOf("TEACHER", "ORGANIZER", "ADMIN") ||
                    profile?.studentId?.startsWith("GV", ignoreCase = true) == true ||
                    profile?.email?.contains("teacher", ignoreCase = true) == true
                state.update { it.copy(isTeacher = isTeacher) }
            }
        }
        viewModelScope.launch {
            useCase.timetable.collect { value ->
                state.update { it.copy(timetable = value, isLoading = false) }
            }
        }
        viewModelScope.launch {
            eventRepository.currentUserRegistrations().collect { ids ->
                registeredEventIds.clear()
                registeredEventIds.addAll(ids)
                reloadEvents()
            }
        }
        viewModelScope.launch {
            eventRepository.getEvents().collect { events ->
                allStoredEvents = events
                reloadEvents()
            }
        }
        viewModelScope.launch {
            while (true) {
                delay(60_000)
                val currentNow = LocalDateTime.now(clock)
                state.update { it.copy(now = currentNow) }
            }
        }
    }

    private fun reloadEvents() {
        val mapped = allStoredEvents.map { it.toScheduleEvent(registeredEventIds) }
        state.update { it.copy(events = mapped) }
    }

    private fun Event.toScheduleEvent(registeredIds: Set<String>): ScheduleEvent {
        val startDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(startAt), ZoneId.systemDefault())
        val endDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(endAt), ZoneId.systemDefault())
        val isRegistered = registeredIds.contains(id)
        val session = when {
            startDateTime.hour < 12 -> "Morning"
            startDateTime.hour < 18 -> "Afternoon"
            else -> "Evening"
        }
        return ScheduleEvent(
            id = id,
            title = title,
            organizer = orgName.ifBlank { "UTH" },
            location = location,
            startAt = startDateTime,
            endAt = endDateTime,
            registration = if (isRegistered) EventRegistration.REGISTERED else EventRegistration.NOT_REGISTERED,
            accent = EventAccent.PRIMARY,
            trainingPoints = "+5 ĐRL",
            session = session,
        )
    }

    fun onAction(action: ScheduleAction) {
        when (action) {
            is ScheduleAction.SelectDate -> state.update {
                it.copy(selectedDate = action.value)
            }

            ScheduleAction.JumpToToday -> {
                val today = state.value.now.toLocalDate()
                state.update { it.copy(selectedDate = today) }
            }

            is ScheduleAction.SelectPeriod -> state.update { it.copy(period = action.value) }

            is ScheduleAction.SelectTab -> state.update { it.copy(tab = action.value) }

            is ScheduleAction.RegisterEvent -> {
                val event = state.value.events.firstOrNull { it.id == action.id } ?: return
                if (!state.value.now.isBefore(event.startAt)) return
                registeredEventIds.add(action.id)
                viewModelScope.launch {
                    eventRepository.registerCurrentUser(action.id)
                }
                reloadEvents()
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

            ScheduleAction.OpenCreateEvent -> state.update { it.copy(showCreateEvent = true) }

            ScheduleAction.CloseCreateEvent -> state.update { it.copy(showCreateEvent = false) }

            ScheduleAction.ToggleTeacherMode -> state.update { it.copy(isTeacher = !it.isTeacher) }

            is ScheduleAction.CreateWorkshopEvent -> {
                val startDateTime = action.date.atTime(
                    runCatching { LocalTime.parse(action.startTime) }.getOrElse { LocalTime.of(8, 30) },
                )
                val endDateTime = action.date.atTime(
                    runCatching { LocalTime.parse(action.endTime) }.getOrElse { LocalTime.of(11, 30) },
                )
                val startMillis = startDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                val endMillis = endDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                val eventId = "event-workshop-${System.currentTimeMillis()}"
                val title = action.title.ifBlank { "Workshop: Ứng dụng AI & IoT trong Giao thông Đô thị 2026" }
                val organizer = action.coOrganizers.joinToString(" & ").ifBlank { "Đoàn - Hội CNTT & CLB Sáng tạo Trẻ" }
                val venue = action.venue.ifBlank { "Hội trường: A.101 (Cơ sở 1)" }

                val draft = EventDraft(
                    title = title,
                    description = action.summary.ifBlank { "Workshop do ThS. Trần Thu Trang tổ chức" },
                    location = venue,
                    campus = "Cơ sở 1",
                    startAt = startMillis,
                    endAt = endMillis,
                    capacity = 100,
                )

                val newDomainEvent = Event(
                    id = eventId,
                    title = title,
                    description = draft.description,
                    orgId = "GV2023001",
                    orgName = organizer,
                    location = venue,
                    campus = "Cơ sở 1",
                    startAt = startMillis,
                    endAt = endMillis,
                    capacity = 100,
                    registeredCount = 0,
                    status = EventStatus.PUBLISHED,
                )

                allStoredEvents = allStoredEvents + newDomainEvent

                viewModelScope.launch {
                    eventRepository.saveEvent(eventId, draft)
                }

                state.update { current ->
                    current.copy(
                        showCreateEvent = false,
                        selectedDate = action.date,
                    )
                }
                reloadEvents()
            }

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
