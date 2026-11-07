package com.giathinh.uthconnect.presentation.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.giathinh.uthconnect.domain.model.UserRole
import com.giathinh.uthconnect.domain.usecase.GetProfileUseCase
import com.giathinh.uthconnect.domain.usecase.TimetableUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class ScheduleViewModel @Inject constructor(
    private val useCase: TimetableUseCase,
    @param:Named("timetable") private val clock: Clock,
    getProfile: GetProfileUseCase? = null,
) : ViewModel() {
    private val registeredEventIds = mutableSetOf("event-career")

    private val initialNow = LocalDateTime.now(clock)
    private val initialDate = initialNow.toLocalDate()

    private val state = MutableStateFlow(
        ScheduleUiState(
            now = initialNow,
            selectedDate = initialDate,
            events = sampleEvents(initialDate, initialNow),
        ),
    )
    val uiState = state.asStateFlow()

    init {
        if (getProfile != null) {
            viewModelScope.launch {
                getProfile.observe().collect { profile ->
                    val isTeacher = profile?.role?.isTeacherOrOrganizer == true ||
                        profile?.role?.name in listOf("TEACHER", "ORGANIZER", "ADMIN") ||
                        profile?.studentId?.startsWith("GV", ignoreCase = true) == true ||
                        profile?.email?.contains("teacher", ignoreCase = true) == true
                    state.update { it.copy(isTeacher = isTeacher) }
                }
            }
        }
        viewModelScope.launch {
            useCase.timetable.collect { value ->
                state.update { it.copy(timetable = value, isLoading = false) }
            }
        }
        viewModelScope.launch {
            while (true) {
                delay(60_000)
                val currentNow = LocalDateTime.now(clock)
                state.update {
                    it.copy(
                        now = currentNow,
                        events = sampleEvents(it.selectedDate, currentNow),
                    )
                }
            }
        }
    }

    fun onAction(action: ScheduleAction) {
        when (action) {
            is ScheduleAction.SelectDate -> state.update {
                it.copy(
                    selectedDate = action.value,
                    events = sampleEvents(action.value, it.now),
                )
            }

            ScheduleAction.JumpToToday -> {
                val today = state.value.now.toLocalDate()
                state.update {
                    it.copy(
                        selectedDate = today,
                        events = sampleEvents(today, it.now),
                    )
                }
            }

            is ScheduleAction.SelectPeriod -> state.update { it.copy(period = action.value) }

            is ScheduleAction.SelectTab -> state.update { it.copy(tab = action.value) }

            is ScheduleAction.RegisterEvent -> {
                val event = state.value.events.firstOrNull { it.id == action.id } ?: return
                if (!state.value.now.isBefore(event.startAt)) return
                registeredEventIds.add(action.id)
                state.update { current ->
                    current.copy(events = sampleEvents(current.selectedDate, current.now))
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
                val newEvent = ScheduleEvent(
                    id = "event-workshop-${System.currentTimeMillis()}",
                    title = action.title.ifBlank { "Workshop: Ứng dụng AI & IoT trong Giao thông Đô thị 2026" },
                    organizer = action.coOrganizers.joinToString(" & ").ifBlank { "Đoàn - Hội CNTT & CLB Sáng tạo Trẻ" },
                    location = action.venue.ifBlank { "Hội trường: A.101 (Cơ sở 1)" },
                    startAt = startDateTime,
                    endAt = endDateTime,
                    registration = EventRegistration.NOT_REGISTERED,
                    accent = EventAccent.PRIMARY,
                    trainingPoints = action.trainingPoints.ifBlank { "+5 ĐRL" },
                    session = when {
                        startDateTime.hour < 12 -> "Morning"
                        startDateTime.hour < 18 -> "Afternoon"
                        else -> "Evening"
                    },
                )
                state.update { current ->
                    current.copy(
                        showCreateEvent = false,
                        events = listOf(newEvent) + current.events,
                        selectedDate = action.date,
                    )
                }
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

    private fun sampleEvents(date: LocalDate, now: LocalDateTime): List<ScheduleEvent> {
        val isToday = date == now.toLocalDate()

        // Sự kiện 1: Đảm bảo nằm trong khung giờ hiện tại khi mở ngày hôm nay
        val aiStart: LocalDateTime
        val aiEnd: LocalDateTime
        if (isToday) {
            if (now.hour in 8..10) {
                aiStart = date.atTime(8, 0)
                aiEnd = date.atTime(11, 30)
            } else {
                aiStart = now.minusMinutes(45).withSecond(0).withNano(0)
                aiEnd = now.plusMinutes(75).withSecond(0).withNano(0)
            }
        } else {
            aiStart = date.atTime(8, 0)
            aiEnd = date.atTime(11, 30)
        }

        val event1Reg = if (registeredEventIds.contains("event-ai")) EventRegistration.REGISTERED else EventRegistration.CHECKED_IN
        val event2Reg = if (registeredEventIds.contains("event-career")) EventRegistration.REGISTERED else EventRegistration.REGISTERED
        val event3Reg = if (registeredEventIds.contains("event-sports")) EventRegistration.REGISTERED else EventRegistration.NOT_REGISTERED
        val event3Accent = if (event3Reg == EventRegistration.REGISTERED) EventAccent.PRIMARY else EventAccent.WARNING

        val careerStart = if (isToday && aiEnd >= date.atTime(14, 0)) aiEnd.plusMinutes(30) else date.atTime(14, 0)
        val careerEnd = if (isToday && aiEnd >= date.atTime(14, 0)) careerStart.plusHours(3) else date.atTime(17, 0)

        return listOf(
            ScheduleEvent(
                id = "event-ai",
                title = "Workshop: AI & Tương lai ngành Giao thông thông minh",
                organizer = "Đoàn Thanh niên & Khoa CNTT",
                location = "Hội trường: A.101 (Cơ sở chính)",
                startAt = aiStart,
                endAt = aiEnd,
                registration = event1Reg,
                accent = EventAccent.SUCCESS,
                trainingPoints = "+5 ĐRL",
                session = "Morning",
            ),
            ScheduleEvent(
                id = "event-career",
                title = "Ngày hội Việc làm & Kết nối Doanh nghiệp Logistics 2026",
                organizer = "Trung tâm Hướng nghiệp & Tuyển dụng UTH",
                location = "Địa điểm: Sân trung tâm & Hội trường B",
                startAt = careerStart,
                endAt = careerEnd,
                registration = event2Reg,
                accent = EventAccent.PRIMARY,
                trainingPoints = "+10 ĐRL",
                session = "Afternoon",
            ),
            ScheduleEvent(
                id = "event-sports",
                title = "Hội thao Sinh viên UTH 2026",
                organizer = "CLB Thể Thao UTH",
                location = "Địa điểm: Sân vận động Cơ sở 2",
                startAt = date.atTime(18, 30),
                endAt = date.atTime(21, 0),
                registration = event3Reg,
                accent = event3Accent,
                trainingPoints = "+5 ĐRL",
                session = "Evening",
            ),
        )
    }
}
