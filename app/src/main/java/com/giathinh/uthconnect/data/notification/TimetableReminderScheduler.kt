package com.giathinh.uthconnect.data.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import com.giathinh.uthconnect.R
import com.giathinh.uthconnect.domain.model.CourseSchedule
import com.giathinh.uthconnect.domain.model.Deadline
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

class TimetableReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    fun scheduleClass(schedule: CourseSchedule) {
        cancelClass(schedule.id)
        scheduleClassReminder(
            key = "class:${schedule.id}",
            dayOfWeek = schedule.dayOfWeek,
            startTime = schedule.startTime,
            title = schedule.courseName,
            body = context.getString(R.string.timetable_class_reminder_body, schedule.room, schedule.campus),
        )
    }

    fun cancelClass(id: String) = cancel("class:$id")

    fun scheduleDeadline(deadline: Deadline) {
        cancelDeadline(deadline.id)
        val dueAt = deadline.dueDate.atTime(deadline.dueTime)
        planDeadlineReminders(dueAt, LocalDateTime.now()).forEach { reminder ->
            val body = when (reminder.daysBefore) {
                3 -> context.getString(R.string.timetable_deadline_reminder_3_days, deadline.title, dueAt.format(dateTimeFormat))
                1 -> context.getString(R.string.timetable_deadline_reminder_1_day, deadline.title, dueAt.format(dateTimeFormat))
                0 -> context.getString(R.string.timetable_deadline_reminder_due, deadline.title)
                else -> return@forEach
            }
            scheduleAlarm("deadline:${deadline.id}:${reminder.daysBefore}", reminder.triggerAt, deadline.title, body)
        }
    }

    fun cancelDeadline(id: String) {
        listOf(3, 1, 0).forEach { daysBefore -> cancel("deadline:$id:$daysBefore") }
    }

    internal fun scheduleNextClass(
        key: String,
        dayOfWeek: Int,
        startTime: String,
        title: String,
        body: String,
    ) {
        scheduleClassReminder(key, dayOfWeek, startTime, title, body)
    }

    private fun scheduleClassReminder(
        key: String,
        dayOfWeek: Int,
        startTime: String,
        title: String,
        body: String,
    ) {
        scheduleAlarm(
            key = key,
            triggerAt = nextClassReminder(dayOfWeek, startTime, LocalDateTime.now()),
            title = title,
            body = body,
            repeatDay = dayOfWeek,
            repeatStartTime = startTime,
        )
    }

    private fun scheduleAlarm(
        key: String,
        triggerAt: LocalDateTime,
        title: String,
        body: String,
        repeatDay: Int? = null,
        repeatStartTime: String? = null,
    ) {
        val intent = Intent(context, TimetableReminderReceiver::class.java).apply {
            action = ACTION_SHOW_REMINDER
            data = Uri.parse("uthconnect://reminder/${Uri.encode(key)}")
            putExtra(EXTRA_KEY, key)
            putExtra(EXTRA_TITLE, title)
            putExtra(EXTRA_BODY, body)
            putExtra(EXTRA_REPEAT_DAY, repeatDay ?: -1)
            putExtra(EXTRA_REPEAT_START, repeatStartTime)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            key.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val triggerMillis = triggerAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager.canScheduleExactAlarms()) {
            runCatching { alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent) }
                .onFailure { alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent) }
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
        }
    }

    private fun cancel(key: String) {
        val intent = Intent(context, TimetableReminderReceiver::class.java).apply {
            action = ACTION_SHOW_REMINDER
            data = Uri.parse("uthconnect://reminder/${Uri.encode(key)}")
        }
        PendingIntent.getBroadcast(
            context,
            key.hashCode(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        )?.let(alarmManager::cancel)
    }

    companion object {
        const val ACTION_SHOW_REMINDER = "com.giathinh.uthconnect.SHOW_TIMETABLE_REMINDER"
        const val EXTRA_KEY = "reminder_key"
        const val EXTRA_TITLE = "reminder_title"
        const val EXTRA_BODY = "reminder_body"
        const val EXTRA_REPEAT_DAY = "repeat_day"
        const val EXTRA_REPEAT_START = "repeat_start"
        private val dateTimeFormat = DateTimeFormatter.ofPattern("dd/MM HH:mm")
    }
}
