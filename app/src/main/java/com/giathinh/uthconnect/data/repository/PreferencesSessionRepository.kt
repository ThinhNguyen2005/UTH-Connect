package com.giathinh.uthconnect.data.repository

import android.content.Context
import android.util.Log
import com.giathinh.uthconnect.data.push.FcmTokenRegistrar
import com.giathinh.uthconnect.domain.model.StudentProfile
import com.giathinh.uthconnect.domain.repository.AuthRepository
import com.giathinh.uthconnect.domain.repository.SessionFailure
import com.giathinh.uthconnect.domain.repository.SessionRepository
import com.giathinh.uthconnect.domain.repository.StudentSession
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

/** Only student ID / biometric opt-in; never password, raw biometric, session token. */
@Singleton
class PreferencesSessionRepository @Inject constructor(
    @ApplicationContext context: Context,
    private val auth: AuthRepository,
    private val client: SupabaseRestClient,
    private val supabaseAuth: SupabaseAuthRepository,
    private val secureStore: SecureSessionStore,
    private val fcmTokenRegistrar: FcmTokenRegistrar,
) : SessionRepository {
    private val prefs = context.getSharedPreferences("uth_auth_demo", Context.MODE_PRIVATE)
    private val mutex = Mutex()
    private val _session = MutableStateFlow(StudentSession())
    override val session: StateFlow<StudentSession> = _session.asStateFlow()
    private var restored = false

    init {
        client.attachSecureStore(secureStore)
    }

    override suspend fun restore() {
        mutex.withLock {
            if (restored) return
            restored = true
            try {
                val hasSession = client.restoreSavedSession()
                val candidateProfile = if (hasSession) supabaseAuth.currentVerifiedProfile() else null
                _session.value = StudentSession(
                    isRestoring = false,
                    profile = candidateProfile,
                    remembered = hasSession,
                )
                if (candidateProfile != null) fcmTokenRegistrar.registerForSignedInUser()
            } catch (cancelled: CancellationException) {
                restored = false
                throw cancelled
            } catch (_: Exception) {
                restored = false // Allow retry after connectivity/timeout failures.
                _session.value = StudentSession(isRestoring = false, failure = SessionFailure.RestoreFailed)
            }
        }
    }

    override suspend fun start(profile: StudentProfile, remember: Boolean) {
        mutex.withLock {
            client.setRememberSession(remember)
            check(prefs.edit().remove(KEY_ID).remove(KEY_BIO_ID).commit()) { "Cannot clear old preferences" }
            _session.value = StudentSession(
                isRestoring = false,
                profile = profile,
                remembered = remember,
            )
        }
        fcmTokenRegistrar.registerForSignedInUser()
    }

    override suspend fun updateProfile(profile: StudentProfile) {
        mutex.withLock {
            check(_session.value.profile?.studentId == profile.studentId) { "No active session" }
            _session.value = _session.value.copy(profile = profile)
        }
    }

    override suspend fun enableBiometric(studentId: String) {
        mutex.withLock {
            error("Biometric login is not enabled without a signed-in Supabase session")
        }
    }

    override suspend fun disableBiometric() {
        mutex.withLock {
            check(prefs.edit().remove(KEY_BIO_ID).commit()) { "Cannot disable biometric preference" }
            _session.value = _session.value.copy(biometricStudentId = null)
        }
    }

    override suspend fun unlockWithBiometric() {
        mutex.withLock {
            error("Biometric unlock is disabled")
        }
    }

    @Suppress("TooGenericExceptionCaught")
    override suspend fun signOut() {
        mutex.withLock {
            // Device cleanup is best effort. Local credentials must still be revoked when
            // the network is unavailable; a later login reclaims the installation row.
            val cleanupCompleted = withContext(kotlinx.coroutines.NonCancellable) {
                withTimeoutOrNull(PUSH_CLEANUP_TIMEOUT_MS) {
                    try {
                        fcmTokenRegistrar.unregisterCurrentUser(restoreOnFailure = false)
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (error: Exception) {
                        Log.w(TAG, "Unable to revoke push device row", error)
                    }
                    true
                }
            } == true
            if (!cleanupCompleted) Log.w(TAG, "Push device cleanup timed out; local sign-out will continue")
            try {
                client.setRememberSession(false)
            } finally {
                try {
                    auth.signOut()
                } finally {
                    _session.value = StudentSession(isRestoring = false)
                    // Explicit log-out REVOKES device remembered sign-in and biometric opt-in.
                    check(prefs.edit().remove(KEY_ID).remove(KEY_BIO_ID).commit()) { "Cannot revoke session" }
                }
            }
        }
    }

    private companion object {
        const val KEY_ID = "remembered_student_id"
        const val KEY_BIO_ID = "biometric_student_id"
        const val TAG = "PreferencesSessionRepository"
        const val PUSH_CLEANUP_TIMEOUT_MS = 5_000L
    }
}
