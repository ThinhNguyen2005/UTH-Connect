package com.giathinh.uthconnect.data.notification

import com.giathinh.uthconnect.domain.model.CourseSchedule
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.TemporalAdjusters

internal data class PlannedReminder(
    val daysBefore: Int,
    val triggerAt: LocalDateTime,
)

internal fun planDeadlineReminders(
    dueAt: LocalDateTime,
    now: LocalDateTime,
): List<PlannedReminder> = listOf(3, 1, 0).mapNotNull { daysBefore ->
    val triggerAt = dueAt.minusDays(daysBefore.toLong())
    if (triggerAt.isAfter(now)) PlannedReminder(daysBefore, triggerAt) else null
}

internal fun nextClassReminder(schedule: CourseSchedule, now: LocalDateTime): LocalDateTime {
    return nextClassReminder(schedule.dayOfWeek, schedule.startTime, now)
}

internal fun nextClassReminder(dayOfWeek: Int, startTime: String, now: LocalDateTime): LocalDateTime {
    val classDay = DayOfWeek.of(dayOfWeek)
    val classTime = LocalTime.parse(startTime)
    val nextStart = now.toLocalDate()
        .with(TemporalAdjusters.nextOrSame(classDay))
        .atTime(classTime)
    val reminder = nextStart.minusMinutes(15)
    return if (reminder.isAfter(now)) reminder else reminder.plusWeeks(1)
}
