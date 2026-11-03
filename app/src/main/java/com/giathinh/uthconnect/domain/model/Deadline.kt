package com.giathinh.uthconnect.domain.model

import java.time.LocalDate

data class Deadline(
    val id: String,
    val title: String,
    val courseName: String,
    val dueDate: LocalDate,
    val note: String,
)
