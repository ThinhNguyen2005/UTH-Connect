package com.giathinh.uthconnect.data.notification

import com.giathinh.uthconnect.domain.model.CourseSchedule
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class ReminderPlannerTest {
    @Test
    fun `deadline reminders include three days one day and due time`() {
        val dueAt = LocalDateTime.of(2026, 11, 10, 23, 59)

        val reminders = planDeadlineReminders(dueAt, LocalDateTime.of(2026, 11, 7, 0, 0))

        assertEquals(listOf(3, 1, 0), reminders.map { it.daysBefore })
        assertEquals(
            listOf(
                LocalDateTime.of(2026, 11, 7, 23, 59),
                LocalDateTime.of(2026, 11, 9, 23, 59),
                dueAt,
            ),
            reminders.map { it.triggerAt },
        )
    }

    @Test
    fun `deadline planner skips reminders that have already passed`() {
        val dueAt = LocalDateTime.of(2026, 11, 10, 23, 59)

        val reminders = planDeadlineReminders(dueAt, LocalDateTime.of(2026, 11, 9, 23, 59))

        assertEquals(listOf(0), reminders.map { it.daysBefore })
    }

    @Test
    fun `class reminder is fifteen minutes before next weekly class`() {
        val schedule = CourseSchedule(
            id = "class-1", courseName = "Mobile", courseCode = "CS101", lecturer = "",
            room = "B102", campus = "Campus 1", dayOfWeek = 1, startPeriod = 1, endPeriod = 3,
            startTime = "07:00", endTime = "09:15", credits = 3,
        )

        assertEquals(
            LocalDateTime.of(2026, 11, 2, 6, 45),
            nextClassReminder(schedule, LocalDateTime.of(2026, 11, 2, 6, 30)),
        )
        assertEquals(
            LocalDateTime.of(2026, 11, 9, 6, 45),
            nextClassReminder(schedule, LocalDateTime.of(2026, 11, 2, 6, 50)),
        )
    }
}
