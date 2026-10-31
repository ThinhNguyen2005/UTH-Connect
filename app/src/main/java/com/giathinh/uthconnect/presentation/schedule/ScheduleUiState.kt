package com.giathinh.uthconnect.presentation.schedule

import com.giathinh.uthconnect.domain.model.Course
import com.giathinh.uthconnect.domain.model.Timetable
import java.time.LocalDateTime
import com.giathinh.uthconnect.domain.usecase.TimetableUseCase

data class ScheduleUiState(
    val timetable: Timetable = Timetable(),
    val now: LocalDateTime = LocalDateTime.MIN,
    val showForm: Boolean = false,
    val course: Course = Course.ANDROID,
    val title: String = "",
    val date: String = "",
    val time: String = "23:59",
    val invalidForm: Boolean = false,
) {
    val tomorrowClasses get() = TimetableUseCase.tomorrow(timetable, now.toLocalDate())
    val urgentDeadlines get() = TimetableUseCase.urgent(timetable, now)
    val laterDeadlines get() = TimetableUseCase.pending(timetable).filter { it.dueAt.isAfter(now.plusHours(48)) }
}
