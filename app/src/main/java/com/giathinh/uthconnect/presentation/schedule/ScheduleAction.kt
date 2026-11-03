package com.giathinh.uthconnect.presentation.schedule

import com.giathinh.uthconnect.domain.model.CourseSchedule
import com.giathinh.uthconnect.domain.model.Deadline
import java.time.LocalDate

sealed interface ScheduleAction {
    data object Retry : ScheduleAction
    data object RefreshReminders : ScheduleAction
    data object ShowClasses : ScheduleAction
    data object ShowDeadlines : ScheduleAction
    data object PreviousWeek : ScheduleAction
    data object NextWeek : ScheduleAction
    data object Today : ScheduleAction
    data class SelectDate(val date: LocalDate) : ScheduleAction
    data class SetWeekView(val enabled: Boolean) : ScheduleAction
    data class SaveSchedule(val schedule: CourseSchedule) : ScheduleAction
    data class DeleteSchedule(val id: String) : ScheduleAction
    data class SaveDeadline(val deadline: Deadline) : ScheduleAction
    data class DeleteDeadline(val id: String) : ScheduleAction
}
