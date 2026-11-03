package com.giathinh.uthconnect.data.local.room

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.giathinh.uthconnect.domain.model.CourseSchedule

@Entity(tableName = "course_schedules")
data class ScheduleEntity(
    @PrimaryKey val id: String,
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
) {
    fun toDomain() = CourseSchedule(
        id, courseName, courseCode, lecturer, room, campus, dayOfWeek,
        startPeriod, endPeriod, startTime, endTime, credits,
    )

    companion object {
        fun fromDomain(schedule: CourseSchedule) = ScheduleEntity(
            schedule.id, schedule.courseName, schedule.courseCode, schedule.lecturer,
            schedule.room, schedule.campus, schedule.dayOfWeek, schedule.startPeriod,
            schedule.endPeriod, schedule.startTime, schedule.endTime, schedule.credits,
        )
    }
}
