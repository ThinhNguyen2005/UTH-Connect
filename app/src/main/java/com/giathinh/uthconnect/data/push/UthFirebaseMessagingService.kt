package com.giathinh.uthconnect.data.push

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.giathinh.uthconnect.MainActivity
import com.giathinh.uthconnect.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/** Displays foreground FCM messages and forwards notification identity to the app's typed nav host. */
@AndroidEntryPoint
class UthFirebaseMessagingService : FirebaseMessagingService() {
    @Inject lateinit var tokenRegistrar: FcmTokenRegistrar

    override fun onNewToken(token: String) {
        tokenRegistrar.onTokenRefreshed(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val notificationId = message.data[PushIntentContract.NOTIFICATION_ID].orEmpty()
        val title = message.notification?.title
            ?: message.data[PushIntentContract.TITLE]
            ?: getString(R.string.notification_title)
        val body = message.notification?.body ?: message.data[PushIntentContract.BODY].orEmpty()
        if (body.isBlank()) return
        createChannel()

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(PushIntentContract.NOTIFICATION_ID, notificationId)
            putExtra(PushIntentContract.TARGET_TYPE, message.data[PushIntentContract.TARGET_TYPE])
            putExtra(PushIntentContract.TARGET_ID, message.data[PushIntentContract.TARGET_ID])
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            notificationId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(this, PushIntentContract.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .build()

        if (
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            NotificationManagerCompat.from(this).notify(notificationId.hashCode(), notification)
        }
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(
                NotificationChannel(
                    PushIntentContract.CHANNEL_ID,
                    getString(R.string.notification_channel_updates),
                    NotificationManager.IMPORTANCE_HIGH,
                ).apply { description = getString(R.string.notification_channel_updates_description) },
            )
        }
    }
}

/** Stable extra keys shared by FCM delivery and the activity's cold-start handler. */
object PushIntentContract {
    const val CHANNEL_ID = "uth_updates"
    const val NOTIFICATION_ID = "notification_id"
    const val TARGET_TYPE = "target_type"
    const val TARGET_ID = "target_id"
    const val TITLE = "title"
    const val BODY = "body"
}
