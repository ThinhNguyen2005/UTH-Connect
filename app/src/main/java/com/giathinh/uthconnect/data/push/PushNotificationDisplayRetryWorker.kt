package com.giathinh.uthconnect.data.push

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.giathinh.uthconnect.MainActivity
import com.giathinh.uthconnect.R
import com.giathinh.uthconnect.data.repository.SecureSessionStore
import com.giathinh.uthconnect.data.repository.SupabaseRestClient
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.TimeUnit

/** Payload saved with a delayed push so it can be validated after session restoration. */
internal data class PushNotificationPayload(
    val recipientId: String,
    val notificationId: String,
    val title: String,
    val body: String,
    val targetType: String,
    val targetId: String,
) {
    companion object {
        fun from(data: Data): PushNotificationPayload? {
            val recipientId = data.getString(KEY_RECIPIENT_ID)
            val notificationId = data.getString(KEY_NOTIFICATION_ID)
            return if (recipientId == null || notificationId == null) {
                null
            } else {
                PushNotificationPayload(
                    recipientId = recipientId,
                    notificationId = notificationId,
                    title = data.getString(KEY_TITLE).orEmpty(),
                    body = data.getString(KEY_BODY).orEmpty(),
                    targetType = data.getString(KEY_TARGET_TYPE).orEmpty(),
                    targetId = data.getString(KEY_TARGET_ID).orEmpty(),
                )
            }
        }

        private const val KEY_RECIPIENT_ID = "recipient_id"
        private const val KEY_NOTIFICATION_ID = "notification_id"
        private const val KEY_TITLE = "title"
        private const val KEY_BODY = "body"
        private const val KEY_TARGET_TYPE = "target_type"
        private const val KEY_TARGET_ID = "target_id"

        internal fun toWorkData(payload: PushNotificationPayload) = workDataOf(
            KEY_RECIPIENT_ID to payload.recipientId,
            KEY_NOTIFICATION_ID to payload.notificationId,
            KEY_TITLE to payload.title,
            KEY_BODY to payload.body,
            KEY_TARGET_TYPE to payload.targetType,
            KEY_TARGET_ID to payload.targetId,
        )
    }
}

/** Revalidates the intended account before displaying a notification after session restore. */
class PushNotificationDisplayRetryWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val payload = PushNotificationPayload.from(inputData)
        return if (payload == null) Result.failure() else process(payload)
    }

    private suspend fun process(payload: PushNotificationPayload): Result {
        val entryPoint =
            EntryPointAccessors.fromApplication(
                applicationContext,
                PushNotificationRetryEntryPoint::class.java,
            )
        val client = entryPoint.supabaseClient()
        val coordinator = entryPoint.pushNotificationCoordinator()
        val state =
            resolveRecipient(
                payload.recipientId,
                client,
                entryPoint.secureSessionStore(),
            )
        return when (state) {
            RecipientState.CURRENT -> showIfAllowed(payload, client, coordinator)
            RecipientState.STALE -> Result.success()
            RecipientState.RETRY -> Result.retry()
        }
    }

    private suspend fun resolveRecipient(
        expectedUserId: String,
        client: SupabaseRestClient,
        sessionStore: SecureSessionStore,
    ): RecipientState {
        val currentUserId = client.userIdOrNull
        return if (currentUserId == null) {
            restoreRecipient(expectedUserId, client, sessionStore)
        } else {
            recipientState(currentUserId, expectedUserId)
        }
    }

    private suspend fun restoreRecipient(
        expectedUserId: String,
        client: SupabaseRestClient,
        sessionStore: SecureSessionStore,
    ): RecipientState {
        client.attachSecureStore(sessionStore)
        val sessionState = rememberedSessionState(expectedUserId, sessionStore)
        return when (sessionState) {
            RecipientState.CURRENT -> restoreAndCheck(expectedUserId, client)
            RecipientState.STALE -> RecipientState.STALE
            RecipientState.RETRY -> RecipientState.RETRY
        }
    }

    private fun rememberedSessionState(
        expectedUserId: String,
        sessionStore: SecureSessionStore,
    ): RecipientState = try {
        val rememberedUserId = sessionStore.load()?.userId
        if (rememberedUserId == expectedUserId) RecipientState.CURRENT else RecipientState.STALE
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (_: Exception) {
        RecipientState.RETRY
    }

    private suspend fun restoreAndCheck(
        expectedUserId: String,
        client: SupabaseRestClient,
    ): RecipientState = try {
        withTimeoutOrNull(SESSION_RESTORE_TIMEOUT_MS) {
            client.restoreSavedSession(SESSION_RESTORE_TIMEOUT_MS.toInt())
        }
        recipientState(client.userIdOrNull, expectedUserId)
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (_: Exception) {
        RecipientState.RETRY
    }

    private fun recipientState(
        actualUserId: String?,
        expectedUserId: String,
    ): RecipientState = when {
        actualUserId == expectedUserId -> RecipientState.CURRENT
        actualUserId == null -> RecipientState.RETRY
        else -> RecipientState.STALE
    }

    private fun showIfAllowed(
        payload: PushNotificationPayload,
        client: SupabaseRestClient,
        coordinator: PushNotificationCoordinator,
    ): Result {
        if (!hasNotificationPermission(applicationContext)) return Result.success()
        createChannel(applicationContext)
        val intent =
            Intent(applicationContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(PushIntentContract.NOTIFICATION_ID, payload.notificationId)
                putExtra(PushIntentContract.TARGET_TYPE, payload.targetType)
                putExtra(PushIntentContract.TARGET_ID, payload.targetId)
            }
        val pendingIntent =
            PendingIntent.getActivity(
                applicationContext,
                payload.notificationId.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        val notification =
            NotificationCompat
                .Builder(applicationContext, PushIntentContract.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(payload.title)
                .setContentText(payload.body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(payload.body))
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .build()
        coordinator.notifyForCurrentUser(
            payload.recipientId,
            { client.userIdOrNull },
            payload.notificationId.hashCode(),
            notification,
        )
        return Result.success()
    }

    private enum class RecipientState {
        CURRENT,
        STALE,
        RETRY,
    }

    companion object {
        private const val UNIQUE_WORK_PREFIX = "push-notification-display-"
        private const val SESSION_RESTORE_TIMEOUT_MS = 15_000L

        internal fun enqueue(
            context: Context,
            payload: PushNotificationPayload,
        ) {
            val request =
                OneTimeWorkRequestBuilder<PushNotificationDisplayRetryWorker>()
                    .setInputData(PushNotificationPayload.toWorkData(payload))
                    .setConstraints(
                        Constraints
                            .Builder()
                            .setRequiredNetworkType(NetworkType.CONNECTED)
                            .build(),
                    ).setBackoffCriteria(
                        BackoffPolicy.EXPONENTIAL,
                        WorkRequest.MIN_BACKOFF_MILLIS,
                        TimeUnit.MILLISECONDS,
                    ).build()
            WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
                UNIQUE_WORK_PREFIX + payload.notificationId,
                ExistingWorkPolicy.KEEP,
                request,
            )
        }

        @Suppress("MaxLineLength")
        private fun hasNotificationPermission(context: Context): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED

        private fun createChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val manager = context.getSystemService(NotificationManager::class.java)
                manager.createNotificationChannel(
                    NotificationChannel(
                        PushIntentContract.CHANNEL_ID,
                        context.getString(R.string.notification_channel_updates),
                        NotificationManager.IMPORTANCE_HIGH,
                    ).apply {
                        description = context.getString(R.string.notification_channel_updates_description)
                    },
                )
            }
        }
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface PushNotificationRetryEntryPoint {
    fun supabaseClient(): SupabaseRestClient

    fun secureSessionStore(): SecureSessionStore

    fun pushNotificationCoordinator(): PushNotificationCoordinator
}
