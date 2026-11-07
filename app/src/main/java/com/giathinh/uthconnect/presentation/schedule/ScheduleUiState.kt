package com.giathinh.uthconnect.presentation.schedule

import com.giathinh.uthconnect.domain.model.Course
import com.giathinh.uthconnect.domain.model.Timetable
import com.giathinh.uthconnect.domain.model.Ticket
import com.giathinh.uthconnect.domain.usecase.TimetableUseCase
import java.time.LocalDate
import java.time.LocalDateTime

/** Immutable presentation state for the schedule, event list, and ticket QR dialog. */
data class ScheduleUiState(
    val timetable: Timetable = Timetable(),
    val now: LocalDateTime = LocalDateTime.MIN,
    val isLoading: Boolean = true,
    val selectedDate: LocalDate = now.toLocalDate(),
    val period: SchedulePeriod = SchedulePeriod.ALL,
    val tab: ScheduleTab = ScheduleTab.EVENTS,
    val events: List<ScheduleEvent> = emptyList(),
    val isEventsLoading: Boolean = true,
    val eventsLoadFailed: Boolean = false,
    val eventSaveFailed: Boolean = false,
    val selectedTicket: Ticket? = null,
    val ticketQrToken: String? = null,
    val ticketQrError: String? = null,
    val ticketQrLoading: Boolean = false,
    val studentName: String? = null,
    val showForm: Boolean = false,
    val isTeacher: Boolean = false,
    val showCreateEvent: Boolean = false,
    val course: Course = Course.ANDROID,
    val title: String = "",
    val date: String = "",
    val time: String = "23:59",
    val titleError: Boolean = false,
    val dateError: Boolean = false,
    val timeError: Boolean = false,
) {
    val visibleClasses get() = timetable.sessions
        .filter { it.startsAt.toLocalDate() == selectedDate }
        .filter { period == SchedulePeriod.ALL || SchedulePeriod.fromHour(it.startsAt.hour) == period }
        .sortedBy { it.startsAt }
    val tomorrowClasses get() = TimetableUseCase.tomorrow(timetable, now.toLocalDate())
    val urgentDeadlines get() = TimetableUseCase.urgent(timetable, now)
    val laterDeadlines get() = TimetableUseCase.pending(timetable).filterNot {
        TimetableUseCase.isUrgent(it.dueAt, now)
    }
}

/** Tabs available in the schedule feature. */
enum class ScheduleTab { EVENTS, DEADLINES }

/** Event fields formatted specifically for schedule cards. */
data class ScheduleEvent(
    val id: String,
    val title: String,
    val organizer: String,
    val location: String,
    val startAt: LocalDateTime,
    val endAt: LocalDateTime,
    val registration: EventRegistration,
    val accent: EventAccent,
    val trainingPoints: Int = 0,
    val session: String = "",
)

/** Registration state used to choose event card actions and labels. */
enum class EventRegistration { CHECKED_IN, REGISTERED, NOT_REGISTERED }

/** Semantic accent applied to an event card. */
enum class EventAccent { SUCCESS, PRIMARY, WARNING }

/** Time-of-day filters for class sessions. */
enum class SchedulePeriod {
    ALL,
    MORNING,
    AFTERNOON,
    EVENING,
    ;

    companion object {
        /** Returns the period containing [hour] in 24-hour local time. */
        fun fromHour(hour: Int) = when {
            hour < 12 -> MORNING
            hour < 18 -> AFTERNOON
            else -> EVENING
        }
    }
}
