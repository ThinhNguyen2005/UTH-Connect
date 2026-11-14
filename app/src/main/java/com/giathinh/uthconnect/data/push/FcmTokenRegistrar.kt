package com.giathinh.uthconnect.data.push

import android.content.Context
import com.giathinh.uthconnect.data.repository.SupabaseRestClient
import com.giathinh.uthconnect.data.repository.urlEncode
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** Registers the current Android installation's FCM token against its signed-in Supabase user. */
@Singleton
class FcmTokenRegistrar @Inject constructor(
    @ApplicationContext private val context: Context,
    private val client: SupabaseRestClient,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val registrationMutex = Mutex()

    @Volatile private var registrationEnabled = false

    @Volatile private var registrationJob: Job? = null
    private val installationId: String by lazy {
        val preferences = context.getSharedPreferences("uth_push", Context.MODE_PRIVATE)
        preferences.getString(KEY_INSTALLATION_ID, null) ?: UUID.randomUUID().toString().also { id ->
            preferences.edit().putString(KEY_INSTALLATION_ID, id).apply()
        }
    }

    /** Queues token registration after login or session restoration without delaying the UI. */
    fun registerForSignedInUser() {
        registrationEnabled = true
        registrationJob?.cancel()
        registrationJob = scope.launch {
            retryRegistration {
                registerToken(FirebaseMessaging.getInstance().token.await())
            }
        }
    }

    /** Queues a token rotation update received by FirebaseMessagingService. */
    fun onTokenRefreshed(token: String) {
        if (!registrationEnabled) return
        registrationJob?.cancel()
        registrationJob = scope.launch { retryRegistration { registerToken(token) } }
    }

    /** Deletes this installation's token row before sign-out while auth credentials remain valid. */
    @Suppress("TooGenericExceptionCaught")
    suspend fun unregisterCurrentUser(restoreOnFailure: Boolean = true) {
        registrationEnabled = false
        registrationJob?.cancel()
        registrationJob?.join()
        registrationJob = null
        try {
            registrationMutex.withLock {
                val userId = client.userIdOrNull?.let(::urlEncode) ?: return@withLock
                val deviceId = urlEncode(deviceId())
                client.deleteTable("notification_devices?device_id=eq.$deviceId&user_id=eq.$userId")
            }
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            if (restoreOnFailure) restoreRegistrationIfSessionActive()
            throw cancelled
        } catch (error: Exception) {
            if (restoreOnFailure) restoreRegistrationIfSessionActive()
            throw error
        }
    }

    private fun restoreRegistrationIfSessionActive() {
        if (client.userIdOrNull != null) registerForSignedInUser()
    }

    private suspend fun registerToken(token: String) {
        if (token.isBlank()) return
        registrationMutex.withLock {
            if (!registrationEnabled) return
            client.rpc(
                "claim_notification_device",
                buildJsonObject {
                    put("device_id", deviceId())
                    put("token", token)
                    put("platform", "ANDROID")
                },
            )
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun retryRegistration(register: suspend () -> Unit) {
        var lastError: Throwable? = null
        repeat(MAX_ATTEMPTS) { attempt ->
            try {
                register()
                return
            } catch (cancellation: kotlinx.coroutines.CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                lastError = error
                if (attempt < MAX_ATTEMPTS - 1) {
                    delay(RETRY_DELAY_MS * (1L shl attempt))
                }
            }
        }
        android.util.Log.w(TAG, "FCM token registration deferred after retries", lastError)
    }

    private fun deviceId(): String = installationId

    private companion object {
        const val KEY_INSTALLATION_ID = "installation_id"
        const val MAX_ATTEMPTS = 3
        const val RETRY_DELAY_MS = 1_000L
        const val TAG = "FcmTokenRegistrar"
    }
}
