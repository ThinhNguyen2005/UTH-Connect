package com.giathinh.uthconnect.domain.model

import java.time.LocalDate
import java.time.LocalTime

data class Deadline(
    val id: String,
    val title: String,
    val courseName: String,
    val dueDate: LocalDate,
    val note: String,
    val dueTime: LocalTime = LocalTime.of(23, 59),
)
