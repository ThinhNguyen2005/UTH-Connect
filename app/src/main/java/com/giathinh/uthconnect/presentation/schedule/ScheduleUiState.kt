package com.giathinh.uthconnect.presentation.schedule

import androidx.compose.runtime.Immutable
import com.giathinh.uthconnect.domain.model.CourseSchedule
import com.giathinh.uthconnect.domain.model.Deadline
import java.time.LocalDate

enum class ScheduleTab { Classes, Deadlines }

enum class ScheduleFailure { LoadFailed, SaveFailed }

@Immutable
data class ScheduleUiState(
    val schedules: List<CourseSchedule> = emptyList(),
    val deadlines: List<Deadline> = emptyList(),
    val selectedDate: LocalDate = LocalDate.now(),
    val selectedTab: ScheduleTab = ScheduleTab.Classes,
    val isWeekView: Boolean = false,
    val isLoading: Boolean = true,
    val failure: ScheduleFailure? = null,
)
