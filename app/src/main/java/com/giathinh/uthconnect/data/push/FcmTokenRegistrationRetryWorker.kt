package com.giathinh.uthconnect.data.push

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit

internal enum class FcmRegistrationRetryResult {
    COMPLETE,
    RETRY,
    STALE_SESSION,
}

/** Retries FCM token registration durably after the app process is stopped. */
class FcmTokenRegistrationRetryWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val expectedUserId = inputData.getString(KEY_EXPECTED_USER_ID) ?: return Result.failure()
        val registrar = EntryPointAccessors.fromApplication(
            applicationContext,
            FcmTokenRegistrationRetryEntryPoint::class.java,
        ).fcmTokenRegistrar()
        return when (registrar.retryRegistrationFromWorker(expectedUserId)) {
            FcmRegistrationRetryResult.COMPLETE -> Result.success()
            FcmRegistrationRetryResult.RETRY -> Result.retry()
            FcmRegistrationRetryResult.STALE_SESSION -> Result.success()
        }
    }

    companion object {
        private const val KEY_EXPECTED_USER_ID = "expected_user_id"
        private const val UNIQUE_WORK_NAME = "fcm-token-registration-retry"
        private const val INITIAL_DELAY_MS = 30_000L

        fun enqueue(context: Context, expectedUserId: String) {
            val request = OneTimeWorkRequestBuilder<FcmTokenRegistrationRetryWorker>()
                .setInputData(workDataOf(KEY_EXPECTED_USER_ID to expectedUserId))
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build(),
                )
                .setInitialDelay(INITIAL_DELAY_MS, TimeUnit.MILLISECONDS)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS,
                )
                .build()
            WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
                UNIQUE_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request,
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context.applicationContext).cancelUniqueWork(UNIQUE_WORK_NAME)
        }
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface FcmTokenRegistrationRetryEntryPoint {
    fun fcmTokenRegistrar(): FcmTokenRegistrar
}
