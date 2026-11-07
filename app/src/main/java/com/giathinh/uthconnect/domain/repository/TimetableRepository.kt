package com.giathinh.uthconnect.domain.repository

import com.giathinh.uthconnect.domain.model.Course
import com.giathinh.uthconnect.domain.model.Timetable
import kotlinx.coroutines.flow.StateFlow
import java.time.LocalDateTime

interface TimetableRepository {
    val timetable: StateFlow<Timetable>
    fun addDeadline(course: Course, title: String, dueAt: LocalDateTime)
    fun completeDeadline(id: String)
}
