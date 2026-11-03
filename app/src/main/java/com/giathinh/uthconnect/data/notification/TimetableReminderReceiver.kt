package com.giathinh.uthconnect.data.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.giathinh.uthconnect.R

class TimetableReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra(TimetableReminderScheduler.EXTRA_TITLE) ?: return
        val body = intent.getStringExtra(TimetableReminderScheduler.EXTRA_BODY).orEmpty()
        val repeatDay = intent.getIntExtra(TimetableReminderScheduler.EXTRA_REPEAT_DAY, -1)
        if (repeatDay in 1..7) {
            val key = intent.getStringExtra(TimetableReminderScheduler.EXTRA_KEY) ?: return
            val start = intent.getStringExtra(TimetableReminderScheduler.EXTRA_REPEAT_START) ?: return
            TimetableReminderScheduler(context).scheduleNextClass(key, repeatDay, start, title, body)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return

        val notifications = context.getSystemService(NotificationManager::class.java)
        if (notifications.getNotificationChannel(CHANNEL_ID) == null) {
            notifications.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.timetable_reminder_channel),
                    NotificationManager.IMPORTANCE_DEFAULT,
                ),
            )
        }
        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val contentIntent = launchIntent?.let {
            PendingIntent.getActivity(
                context,
                title.hashCode(),
                it,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(title.hashCode(), notification) }
    }

    companion object {
        private const val CHANNEL_ID = "timetable_reminders"
    }
}
