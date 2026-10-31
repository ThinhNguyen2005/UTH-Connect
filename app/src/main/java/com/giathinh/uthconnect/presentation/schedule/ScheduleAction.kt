package com.giathinh.uthconnect.presentation.schedule

import com.giathinh.uthconnect.domain.model.Course

sealed interface ScheduleAction {
    data object OpenForm : ScheduleAction
    data object CloseForm : ScheduleAction
    data object Save : ScheduleAction
    data class Title(val value: String) : ScheduleAction
    data class Date(val value: String) : ScheduleAction
    data class Time(val value: String) : ScheduleAction
    data class SelectCourse(val value: Course) : ScheduleAction
    data class Complete(val id: String) : ScheduleAction
}
