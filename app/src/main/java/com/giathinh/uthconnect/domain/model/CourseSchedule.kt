package com.giathinh.uthconnect.domain.model

data class CourseSchedule(
    val id: String,
    val courseName: String,
    val courseCode: String,
    val lecturer: String,
    val room: String,
    val campus: String,
    val dayOfWeek: Int,
    val startPeriod: Int,
    val endPeriod: Int,
    val startTime: String,
    val endTime: String,
    val credits: Int,
)
