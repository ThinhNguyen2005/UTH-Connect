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
        registerNetworkCallback()
    }

    /** Registers the optional network monitor without failing singleton creation. */
    @Suppress("TooGenericExceptionCaught")
    private fun registerNetworkCallback() {
        try {
            connectivityManager?.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    synchronized(registrationStateLock) {
                        if (registrationEnabled && !signOutInProgress && client.userIdOrNull != null) {
                            registerForSignedInUserLocked()
                        } else if (canStartRestoreLocked()) {
                            registrationJob = scope.launch { restoreAndRegisterIfAvailable() }
                        }
                    }
                }
            })
        } catch (error: RuntimeException) {
            android.util.Log.w(TAG, "Unable to register default network callback", error)
        }
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
            retryRegistration(generation, userId) { registerToken(userId) }
        }
    }

    /** Queues a token rotation update received by FirebaseMessagingService. */
    @Suppress("ReturnCount", "UNUSED_PARAMETER")
    fun onTokenRefreshed(token: String) {
        var queuedUserId: String? = null
        while (true) {
            val userIdToQueue = registrationRetryUserId()
            if (userIdToQueue == null) {
                queuedUserId = null
            } else if (userIdToQueue != queuedUserId) {
                enqueueDurableRegistrationRetry(userIdToQueue)
                queuedUserId = userIdToQueue
            }
            if (scheduleRegistrationAfterTokenRefresh(queuedUserId)) return
        }
    }

    private fun registrationRetryUserId(): String? = synchronized(registrationStateLock) {
        if (signOutInProgress) null else client.userIdOrNull ?: rememberedSessionUserId()
    }

    private fun scheduleRegistrationAfterTokenRefresh(queuedUserId: String?): Boolean {
        return synchronized(registrationStateLock) {
            if (signOutInProgress) return@synchronized true
            val currentUserId = client.userIdOrNull ?: rememberedSessionUserId()
            if (currentUserId != queuedUserId) return@synchronized false

            val signedInUserId = client.userIdOrNull
            if (signedInUserId != null) {
                if (!registrationEnabled) {
                    registerForSignedInUserLocked()
                    return@synchronized true
                }
                val rotationPending = preferences.getBoolean(KEY_ROTATION_PENDING, false)
                if (rotationPending && registrationJob?.isActive == true) return@synchronized true
                val generation = ++registrationGeneration
                pendingRetryJob?.cancel()
                if (!rotationPending) registrationJob?.cancel()
                registrationJob = scope.launch {
                    retryRegistration(generation, signedInUserId) { registerToken(signedInUserId) }
                }
            } else {
                restorePending = true
                if (canStartRestoreLocked()) {
                    registrationJob = scope.launch { restoreAndRegisterIfAvailable() }
                }
            }
            true
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
            FcmTokenRegistrationRetryWorker.cancel(context)
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

    @Suppress("TooGenericExceptionCaught", "ReturnCount", "ThrowsCount")
    private suspend fun registerToken(expectedUserId: String) {
        registrationMutex.withLock {
            if (!registrationEnabled) return
            var tokenToRegister = FirebaseMessaging.getInstance().token.await()
            for (attempt in 0..OWNERSHIP_ROTATION_ATTEMPTS) {
                if (client.userIdOrNull != expectedUserId) return
                if (preferences.getBoolean(KEY_ROTATION_PENDING, false)) {
                    FirebaseMessaging.getInstance().deleteToken().await()
                    rotateInstallationId()
                    tokenToRegister = FirebaseMessaging.getInstance().token.await()
                    preferences.edit().putBoolean(KEY_ROTATION_PENDING, false).apply()
                }
                if (client.userIdOrNull != expectedUserId) return
                if (tokenToRegister.isBlank()) return
                try {
                    client.rpc(
                        "claim_notification_device",
                        buildJsonObject {
                            put("p_device_id", deviceId())
                            put("p_token", tokenToRegister)
                            put("p_platform", "ANDROID")
                        },
                        expectedUserId = expectedUserId,
                    )
                    return
                } catch (cancellation: kotlinx.coroutines.CancellationException) {
                    throw cancellation
                } catch (error: Exception) {
                    if (!error.message.orEmpty().contains(DEVICE_OWNERSHIP_REQUIRED)) throw error
                    markIdentityRotationPending()
                    if (attempt == OWNERSHIP_ROTATION_ATTEMPTS) throw error
                }
            }
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
        val durableRetryQueued = enqueueDurableRegistrationRetry(userId)
        if (!durableRetryQueued) {
            pendingRetryJob = scope.launch {
                delay(PENDING_RETRY_DELAY_MS)
                synchronized(registrationStateLock) {
                    if (isRegistrationActiveLocked(generation, userId)) registerForSignedInUserLocked()
                }
            }
        }
    }

    @Suppress("TooGenericExceptionCaught", "ReturnCount")
    internal suspend fun retryRegistrationFromWorker(expectedUserId: String): FcmRegistrationRetryResult {
        if (client.userIdOrNull == null) {
            val restored = try {
                client.restoreSavedSession()
            } catch (cancellation: kotlinx.coroutines.CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                android.util.Log.w(TAG, "Unable to restore session for durable FCM registration retry", error)
                return FcmRegistrationRetryResult.RETRY
            }
            if (!restored) return FcmRegistrationRetryResult.STALE_SESSION
        }

        val generation = synchronized(registrationStateLock) {
            if (signOutInProgress || client.userIdOrNull != expectedUserId) {
                return FcmRegistrationRetryResult.STALE_SESSION
            }
            registrationEnabled = true
            pendingRetryJob?.cancel()
            pendingRetryJob = null
            ++registrationGeneration
        }

        return try {
            if (!isRegistrationActive(generation, expectedUserId)) {
                return if (isRetrySessionCurrent(expectedUserId)) {
                    FcmRegistrationRetryResult.RETRY
                } else {
                    FcmRegistrationRetryResult.STALE_SESSION
                }
            }
            registerToken(expectedUserId)
            if (isRegistrationActive(generation, expectedUserId)) {
                FcmRegistrationRetryResult.COMPLETE
            } else if (isRetrySessionCurrent(expectedUserId)) {
                FcmRegistrationRetryResult.RETRY
            } else {
                FcmRegistrationRetryResult.STALE_SESSION
            }
        } catch (cancellation: kotlinx.coroutines.CancellationException) {
            throw cancellation
        } catch (error: Exception) {
            android.util.Log.w(TAG, "Durable FCM token registration retry failed", error)
            FcmRegistrationRetryResult.RETRY
        }
    }

    private fun isRegistrationActive(generation: Long, userId: String): Boolean = synchronized(registrationStateLock) {
        isRegistrationActiveLocked(generation, userId)
    }

    private fun isRetrySessionCurrent(userId: String): Boolean = synchronized(registrationStateLock) {
        !signOutInProgress && client.userIdOrNull == userId
    }

    @Suppress("TooGenericExceptionCaught")
    private fun enqueueDurableRegistrationRetry(userId: String): Boolean = try {
        FcmTokenRegistrationRetryWorker.enqueue(context, userId).result.get()
        true
    } catch (error: Exception) {
        android.util.Log.w(TAG, "Unable to confirm durable FCM token registration retry", error)
        false
    }

    @Suppress("TooGenericExceptionCaught")
    private fun rememberedSessionUserId(): String? = try {
        secureSessionStore.load()?.userId
    } catch (error: Exception) {
        android.util.Log.w(TAG, "Unable to read remembered session for FCM token retry", error)
        null
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

    @Suppress("TooGenericExceptionCaught")
    private fun scheduleRestoreRetryLocked() {
        if (signOutInProgress || registrationEnabled || restoreRetryJob?.isActive == true) return
        val expectedUserId = try {
            secureSessionStore.load()?.userId
        } catch (error: Exception) {
            android.util.Log.w(TAG, "Unable to read remembered session for FCM retry", error)
            null
        }
        val enqueueOperation = expectedUserId?.let { userId ->
            try {
                FcmTokenRegistrationRetryWorker.enqueue(context, userId)
            } catch (error: Exception) {
                android.util.Log.w(TAG, "Unable to enqueue durable FCM session restore retry", error)
                null
            }
        }

        restoreRetryJob = scope.launch {
            val durableRetryQueued = enqueueOperation?.let { operation ->
                try {
                    operation.result.get()
                    true
                } catch (error: Exception) {
                    android.util.Log.w(TAG, "Durable FCM session restore retry enqueue failed", error)
                    false
                }
            } == true
            if (!durableRetryQueued) {
                delay(PENDING_RETRY_DELAY_MS)
                synchronized(registrationStateLock) {
                    restoreRetryJob = null
                    if (canStartRestoreLocked()) {
                        registrationJob = scope.launch { restoreAndRegisterIfAvailable() }
                    }
                }
            }
        }
    }

    private fun canStartRestoreLocked(): Boolean = restorePending && !signOutInProgress &&
        registrationJob?.isActive != true && restoreRetryJob?.isActive != true

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
        const val OWNERSHIP_ROTATION_ATTEMPTS = 1
        const val DEVICE_OWNERSHIP_REQUIRED = "DEVICE_OWNERSHIP_REQUIRED"
        const val TAG = "FcmTokenRegistrar"
    }
}
