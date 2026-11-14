package com.giathinh.uthconnect.data.push

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import com.giathinh.uthconnect.data.repository.SecureSessionStore
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
@Suppress("TooManyFunctions")
class FcmTokenRegistrar @Inject constructor(
    @ApplicationContext private val context: Context,
    private val client: SupabaseRestClient,
    private val secureSessionStore: SecureSessionStore,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val registrationMutex = Mutex()
    private val registrationStateLock = Any()

    @Volatile private var registrationEnabled = false

    @Volatile private var signOutInProgress = false

    @Volatile private var restorePending = false
    private var registrationGeneration = 0L

    @Volatile private var registrationJob: Job? = null

    @Volatile private var pendingRetryJob: Job? = null

    @Volatile private var restoreRetryJob: Job? = null

    private val preferences = context.getSharedPreferences("uth_push", Context.MODE_PRIVATE)
    private val connectivityManager = context.getSystemService(ConnectivityManager::class.java)

    @Volatile private var installationId: String? = null

    init {
        client.attachSecureStore(secureSessionStore)
        connectivityManager?.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                synchronized(registrationStateLock) {
                    if (registrationEnabled && !signOutInProgress && client.userIdOrNull != null) {
                        registerForSignedInUserLocked()
                    } else if (restorePending && !signOutInProgress && registrationJob?.isActive != true &&
                        restoreRetryJob?.isActive != true
                    ) {
                        registrationJob = scope.launch { restoreAndRegisterIfAvailable() }
                    }
                }
            }
        })
    }

    /** Queues token registration after login or session restoration without delaying the UI. */
    fun registerForSignedInUser() {
        synchronized(registrationStateLock) {
            registerForSignedInUserLocked()
        }
    }

    private fun registerForSignedInUserLocked() {
        val userId = client.userIdOrNull ?: return
        signOutInProgress = false
        registrationEnabled = true
        restorePending = false
        val generation = ++registrationGeneration
        pendingRetryJob?.cancel()
        restoreRetryJob?.cancel()
        registrationJob?.cancel()
        registrationJob = scope.launch {
            retryRegistration(generation, userId) {
                registerToken(FirebaseMessaging.getInstance().token.await())
            }
        }
    }

    /** Queues a token rotation update received by FirebaseMessagingService. */
    @Suppress("ReturnCount")
    fun onTokenRefreshed(token: String) {
        synchronized(registrationStateLock) {
            if (registrationEnabled) {
                val userId = client.userIdOrNull ?: return
                val generation = ++registrationGeneration
                pendingRetryJob?.cancel()
                registrationJob?.cancel()
                registrationJob = scope.launch {
                    retryRegistration(generation, userId) { registerToken(token) }
                }
                return
            }
            if (signOutInProgress) return
        }
        synchronized(registrationStateLock) {
            restorePending = true
            if (registrationJob?.isActive != true && restoreRetryJob?.isActive != true) {
                registrationJob = scope.launch { restoreAndRegisterIfAvailable() }
            }
        }
    }

    /** Deletes this installation's token row before sign-out while auth credentials remain valid. */
    @Suppress("TooGenericExceptionCaught")
    suspend fun unregisterCurrentUser(
        restoreOnFailure: Boolean = true,
        requestTimeoutMs: Int? = null,
    ) {
        try {
            val jobToJoin = synchronized(registrationStateLock) {
                if (!restoreOnFailure) signOutInProgress = true
                registrationEnabled = false
                val job = registrationJob
                registrationJob = null
                registrationGeneration++
                restorePending = false
                val pendingJob = pendingRetryJob
                pendingRetryJob = null
                val restoreJob = restoreRetryJob
                restoreRetryJob = null
                job?.cancel()
                pendingJob?.cancel()
                restoreJob?.cancel()
                Triple(job, pendingJob, restoreJob)
            }
            jobToJoin.first?.join()
            jobToJoin.second?.join()
            jobToJoin.third?.join()
            registrationMutex.withLock {
                val userId = client.userIdOrNull?.let(::urlEncode) ?: run {
                    if (!restoreOnFailure) markIdentityRotationPending()
                    return@withLock
                }
                val deviceId = urlEncode(deviceId())
                client.deleteTable(
                    "notification_devices?device_id=eq.$deviceId&user_id=eq.$userId",
                    requestTimeoutMs = requestTimeoutMs,
                )
            }
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            if (!restoreOnFailure) markIdentityRotationPending()
            if (restoreOnFailure) restoreRegistrationIfSessionActive()
            throw cancelled
        } catch (error: Exception) {
            if (!restoreOnFailure) markIdentityRotationPending()
            if (restoreOnFailure) restoreRegistrationIfSessionActive()
            throw error
        }
    }

    private fun restoreRegistrationIfSessionActive() {
        if (client.userIdOrNull != null) registerForSignedInUser()
    }

    private suspend fun registerToken(token: String) {
        registrationMutex.withLock {
            if (!registrationEnabled) return
            val effectiveToken = if (preferences.getBoolean(KEY_ROTATION_PENDING, false)) {
                FirebaseMessaging.getInstance().deleteToken().await()
                rotateInstallationId()
                preferences.edit().putBoolean(KEY_ROTATION_PENDING, false).apply()
                FirebaseMessaging.getInstance().token.await()
            } else {
                token
            }
            if (effectiveToken.isBlank()) return
            client.rpc(
                "claim_notification_device",
                buildJsonObject {
                    put("p_device_id", deviceId())
                    put("p_token", effectiveToken)
                    put("p_platform", "ANDROID")
                },
            )
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun retryRegistration(
        generation: Long,
        userId: String,
        register: suspend () -> Unit,
    ) {
        var lastError: Throwable? = null
        repeat(MAX_ATTEMPTS) { attempt ->
            if (!isRegistrationActive(generation, userId)) return
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
        pendingRetryJob = scope.launch {
            delay(PENDING_RETRY_DELAY_MS)
            synchronized(registrationStateLock) {
                if (isRegistrationActiveLocked(generation, userId)) registerForSignedInUserLocked()
            }
        }
    }

    private fun isRegistrationActive(generation: Long, userId: String): Boolean = synchronized(registrationStateLock) {
        isRegistrationActiveLocked(generation, userId)
    }

    private fun isRegistrationActiveLocked(generation: Long, userId: String): Boolean = registrationEnabled &&
        !signOutInProgress &&
        registrationGeneration == generation &&
        client.userIdOrNull == userId

    @Suppress("TooGenericExceptionCaught")
    private suspend fun restoreAndRegisterIfAvailable() {
        val restored = restoreSavedSessionWithRetry()
        synchronized(registrationStateLock) {
            if (restored && client.userIdOrNull != null && !signOutInProgress) {
                registerForSignedInUserLocked()
            }
        }
    }

    @Suppress("TooGenericExceptionCaught", "ReturnCount")
    private suspend fun restoreSavedSessionWithRetry(): Boolean {
        repeat(MAX_ATTEMPTS) { attempt ->
            synchronized(registrationStateLock) {
                if (signOutInProgress || registrationEnabled) return false
            }
            try {
                val restored = client.restoreSavedSession()
                restorePending = false
                return restored
            } catch (cancellation: kotlinx.coroutines.CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                if (attempt == MAX_ATTEMPTS - 1) {
                    synchronized(registrationStateLock) {
                        restorePending = true
                        scheduleRestoreRetryLocked()
                    }
                    android.util.Log.w(TAG, "Unable to restore remembered session for FCM token", error)
                } else {
                    delay(RETRY_DELAY_MS * (1L shl attempt))
                }
            }
        }
        return false
    }

    private fun scheduleRestoreRetryLocked() {
        if (signOutInProgress || registrationEnabled || restoreRetryJob?.isActive == true) return
        restoreRetryJob = scope.launch {
            delay(PENDING_RETRY_DELAY_MS)
            synchronized(registrationStateLock) {
                restoreRetryJob = null
                if (!signOutInProgress && !registrationEnabled && restorePending &&
                    registrationJob?.isActive != true
                ) {
                    registrationJob = scope.launch { restoreAndRegisterIfAvailable() }
                }
            }
        }
    }

    private fun deviceId(): String = currentInstallationId()

    private fun markIdentityRotationPending() {
        preferences.edit().putBoolean(KEY_ROTATION_PENDING, true).apply()
    }

    private fun rotateInstallationId() {
        val nextId = UUID.randomUUID().toString()
        check(preferences.edit().putString(KEY_INSTALLATION_ID, nextId).commit()) {
            "Cannot rotate push installation ID"
        }
        installationId = nextId
    }

    private fun currentInstallationId(): String {
        installationId?.let { return it }
        val id = preferences.getString(KEY_INSTALLATION_ID, null) ?: UUID.randomUUID().toString().also {
            preferences.edit().putString(KEY_INSTALLATION_ID, it).apply()
        }
        installationId = id
        return id
    }

    private companion object {
        const val KEY_INSTALLATION_ID = "installation_id"
        const val KEY_ROTATION_PENDING = "rotation_pending"
        const val MAX_ATTEMPTS = 3
        const val RETRY_DELAY_MS = 1_000L
        const val PENDING_RETRY_DELAY_MS = 30_000L
        const val TAG = "FcmTokenRegistrar"
    }
}
