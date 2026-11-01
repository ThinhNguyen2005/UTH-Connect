package com.giathinh.uthconnect.data.repository

import com.giathinh.uthconnect.domain.model.ClassSession
import com.giathinh.uthconnect.domain.model.Course
import com.giathinh.uthconnect.domain.model.Deadline
import com.giathinh.uthconnect.domain.model.Timetable
import com.giathinh.uthconnect.domain.repository.TimetableRepository
import java.time.Clock
import java.time.LocalDateTime
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class InMemoryTimetableRepository(clock: Clock) : TimetableRepository {
    private val now = LocalDateTime.now(clock)
    private val tomorrow = now.toLocalDate().plusDays(1)
    private val state = MutableStateFlow(Timetable(
        sessions = (0..4).map { index ->
            val start = tomorrow.plusDays((index / 2).toLong()).atTime(7 + (index % 2) * 6, 0)
            ClassSession("session-$index", Course.entries[index % 3], start, start.plusHours(2), "B10$index")
        },
        deadlines = listOf(
            Deadline("deadline-1", Course.ANDROID, "", now.minusHours(2)),
            Deadline("deadline-2", Course.DATABASE, "", now.plusHours(6)),
            Deadline("deadline-3", Course.ENGLISH, "", now.plusHours(30)),
            Deadline("deadline-4", Course.ANDROID, "", now.plusDays(5)),
        ),
    ))
    override val timetable = state.asStateFlow()
    override fun addDeadline(course: Course, title: String, dueAt: LocalDateTime) {
        require(title.isNotBlank())
        state.update { it.copy(deadlines = it.deadlines + Deadline(UUID.randomUUID().toString(), course, title.trim(), dueAt)) }
    }
    override fun completeDeadline(id: String) {
        state.update { value ->
            value.copy(deadlines = value.deadlines.map { if (it.id == id) it.copy(completed = true) else it })
        }
    }
}
