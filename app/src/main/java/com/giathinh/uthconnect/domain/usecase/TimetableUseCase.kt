package com.giathinh.uthconnect.domain.usecase

import com.giathinh.uthconnect.domain.model.Course
import com.giathinh.uthconnect.domain.model.Timetable
import com.giathinh.uthconnect.domain.repository.TimetableRepository
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject

class TimetableUseCase @Inject constructor(private val repository: TimetableRepository) {
    val timetable = repository.timetable
    fun add(course: Course, title: String, date: String, time: String): Boolean {
        val due = runCatching { LocalDateTime.parse(date.trim() + "T" + time.trim()) }.getOrNull()
        if (title.isBlank() || due == null) return false
        repository.addDeadline(course, title, due)
        return true
    }
    fun complete(id: String) = repository.completeDeadline(id)
    companion object {
        fun tomorrow(value: Timetable, today: LocalDate) =
            value.sessions.filter { it.startsAt.toLocalDate() == today.plusDays(1) }.sortedBy { it.startsAt }
        fun pending(value: Timetable) = value.deadlines.filterNot { it.completed }.sortedBy { it.dueAt }
        fun urgent(value: Timetable, now: LocalDateTime) =
            pending(value).filter { !it.dueAt.isAfter(now.plusHours(48)) }
    }
}
