package com.giathinh.uthconnect.presentation.schedule

import com.giathinh.uthconnect.domain.model.Course
import java.time.LocalDate

sealed interface ScheduleAction {
    data class SelectDate(val value: LocalDate) : ScheduleAction
    data class SelectPeriod(val value: SchedulePeriod) : ScheduleAction
    data class SelectTab(val value: ScheduleTab) : ScheduleAction
    data object OpenForm : ScheduleAction
    data object CloseForm : ScheduleAction
    data object Save : ScheduleAction
    data class Title(val value: String) : ScheduleAction
    data class Date(val value: String) : ScheduleAction
    data class Time(val value: String) : ScheduleAction
    data class SelectCourse(val value: Course) : ScheduleAction
    data class Complete(val id: String) : ScheduleAction
    data class RegisterEvent(val id: String) : ScheduleAction
    data object JumpToToday : ScheduleAction
    data object OpenCreateEvent : ScheduleAction
    data object CloseCreateEvent : ScheduleAction
    data object ToggleTeacherMode : ScheduleAction
    data class CreateWorkshopEvent(
        val title: String,
        val category: String,
        val coOrganizers: List<String>,
        val summary: String,
        val date: LocalDate,
        val startTime: String,
        val endTime: String,
        val venue: String,
        val trainingPoints: String,
        val managers: List<String>,
    ) : ScheduleAction
}
