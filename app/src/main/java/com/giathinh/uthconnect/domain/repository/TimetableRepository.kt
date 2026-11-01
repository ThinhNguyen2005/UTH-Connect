package com.giathinh.uthconnect.domain.repository

import com.giathinh.uthconnect.domain.model.Course
import com.giathinh.uthconnect.domain.model.Timetable
import java.time.LocalDateTime
import kotlinx.coroutines.flow.StateFlow

interface TimetableRepository {
    val timetable: StateFlow<Timetable>
    fun addDeadline(course: Course, title: String, dueAt: LocalDateTime)
    fun completeDeadline(id: String)
}
