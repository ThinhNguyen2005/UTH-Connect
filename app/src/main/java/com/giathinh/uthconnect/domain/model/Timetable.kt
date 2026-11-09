package com.giathinh.uthconnect.domain.model

import java.time.LocalDateTime

/** Supported mock courses; presentation owns their localized labels. */
enum class Course { ANDROID, DATABASE, ENGLISH }

data class ClassSession(
    val id: String,
    val course: Course,
    val startsAt: LocalDateTime,
    val endsAt: LocalDateTime,
    val room: String,
)

data class Deadline(
    val id: String,
    val course: Course,
    val title: String,
    val dueAt: LocalDateTime,
    val completed: Boolean = false,
)

data class Timetable(
    val sessions: List<ClassSession> = emptyList(),
    val deadlines: List<Deadline> = emptyList(),
)
