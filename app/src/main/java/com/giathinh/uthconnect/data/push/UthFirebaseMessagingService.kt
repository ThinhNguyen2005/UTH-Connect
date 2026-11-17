package com.giathinh.uthconnect.data.push

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.giathinh.uthconnect.MainActivity
import com.giathinh.uthconnect.R
import com.giathinh.uthconnect.data.repository.SecureSessionStore
import com.giathinh.uthconnect.data.repository.SupabaseRestClient
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

/** Displays foreground FCM messages and forwards notification identity to the app's typed nav host. */
@AndroidEntryPoint
class UthFirebaseMessagingService : FirebaseMessagingService() {
    @Inject lateinit var tokenRegistrar: FcmTokenRegistrar

    @Inject lateinit var supabaseClient: SupabaseRestClient

    @Inject lateinit var secureSessionStore: SecureSessionStore

    @Inject lateinit var pushNotificationCoordinator: PushNotificationCoordinator

    override fun onNewToken(token: String) {
        tokenRegistrar.onTokenRefreshed(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val recipientId = message.data[PushIntentContract.RECIPIENT_ID].orEmpty()
        if (recipientId.isBlank()) return
        val payload = PushNotificationPayload(
            recipientId = recipientId,
            notificationId = message.data[PushIntentContract.NOTIFICATION_ID].orEmpty(),
            title = message.notification?.title
                ?: message.data[PushIntentContract.TITLE]
                ?: getString(R.string.notification_title),
            body = message.notification?.body ?: message.data[PushIntentContract.BODY].orEmpty(),
            targetType = message.data[PushIntentContract.TARGET_TYPE].orEmpty(),
            targetId = message.data[PushIntentContract.TARGET_ID].orEmpty(),
        )

        when (currentRecipientId()) {
            null -> enqueueRetryForRememberedRecipient(payload)
            recipientId -> displayMessage(payload)
        }
    }

    private fun displayMessage(payload: PushNotificationPayload) {
        createChannel()
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(PushIntentContract.NOTIFICATION_ID, payload.notificationId)
            putExtra(PushIntentContract.TARGET_TYPE, payload.targetType)
            putExtra(PushIntentContract.TARGET_ID, payload.targetId)
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            payload.notificationId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(this, PushIntentContract.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(payload.title)
            .setContentText(payload.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(payload.body))
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
            pushNotificationCoordinator.notifyForCurrentUser(
                payload.recipientId,
                { supabaseClient.userIdOrNull },
                payload.notificationId.hashCode(),
                notification,
            )
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private fun enqueueRetryForRememberedRecipient(payload: PushNotificationPayload) {
        val rememberedUserId = try {
            secureSessionStore.load()?.userId
        } catch (error: Exception) {
            android.util.Log.w(TAG, "Unable to read remembered session for notification retry", error)
            null
        }
        if (rememberedUserId != payload.recipientId) return
        PushNotificationDisplayRetryWorker.enqueue(this, payload)
    }

    /** Restores a remembered session so background FCM delivery can validate its recipient. */
    @Suppress("TooGenericExceptionCaught")
    private fun currentRecipientId(): String? = runBlocking(Dispatchers.IO) {
        try {
            if (supabaseClient.userIdOrNull == null) {
                supabaseClient.attachSecureStore(secureSessionStore)
                withTimeoutOrNull(SESSION_RESTORE_TIMEOUT_MS) {
                    supabaseClient.restoreSavedSession(SESSION_RESTORE_TIMEOUT_MS.toInt())
                }
            }
            supabaseClient.userIdOrNull
        } catch (_: Exception) {
            null
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

    private companion object {
        const val SESSION_RESTORE_TIMEOUT_MS = 5_000L
        const val TAG = "UthFcmMessageService"
    }
}

/** Stable extra keys shared by FCM delivery and the activity's cold-start handler. */
object PushIntentContract {
    const val CHANNEL_ID = "uth_updates"
    const val NOTIFICATION_ID = "notification_id"
    const val RECIPIENT_ID = "recipient_id"
    const val TARGET_TYPE = "target_type"
    const val TARGET_ID = "target_id"
    const val TITLE = "title"
    const val BODY = "body"
}
