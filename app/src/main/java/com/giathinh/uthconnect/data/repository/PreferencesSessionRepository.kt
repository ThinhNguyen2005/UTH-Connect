package com.giathinh.uthconnect.data.repository

import android.content.Context
import com.giathinh.uthconnect.BuildConfig
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
import javax.inject.Inject
import javax.inject.Singleton

/** Only student ID / biometric opt-in; never password, raw biometric, session token. */
@Singleton
class PreferencesSessionRepository @Inject constructor(
    @ApplicationContext context: Context,
    private val auth: AuthRepository,
) : SessionRepository {
    private val prefs = context.getSharedPreferences("uth_auth_demo", Context.MODE_PRIVATE)
    private val mutex = Mutex()
    private val _session = MutableStateFlow(StudentSession())
    override val session: StateFlow<StudentSession> = _session.asStateFlow()
    private var restored = false

    override suspend fun restore() {
        mutex.withLock {
            if (restored) return
            restored = true
            try {
                val biometricId = prefs.getString(KEY_BIO_ID, null)
                val rememberedId = prefs.getString(KEY_ID, null)
                // Biometric opt-in always requires new OS authentication on a cold start.
                val candidateId = biometricId ?: rememberedId
                // A remembered student ID is not an authenticated Supabase session.
                val candidateProfile = if (BuildConfig.USE_SUPABASE) {
                    null
                } else {
                    candidateId?.let { auth.findStudent(it).getOrThrow() }
                }
                if (candidateId != null && candidateProfile == null && !BuildConfig.USE_SUPABASE) {
                    check(prefs.edit().remove(KEY_ID).remove(KEY_BIO_ID).commit())
                }
                _session.value = StudentSession(
                    isRestoring = false,
                    profile = if (biometricId == null) candidateProfile else null,
                    remembered = rememberedId != null && !BuildConfig.USE_SUPABASE,
                    biometricStudentId = if (candidateProfile != null) biometricId else null,
                )
            } catch (cancelled: CancellationException) {
                restored = false
                throw cancelled
            } catch (_: Exception) {
                _session.value = StudentSession(isRestoring = false, failure = SessionFailure.RestoreFailed)
            }
        }
    }

    override suspend fun start(profile: StudentProfile, remember: Boolean) {
        mutex.withLock {
            val edit = prefs.edit()
            if (remember && !BuildConfig.USE_SUPABASE) edit.putString(KEY_ID, profile.studentId) else edit.remove(KEY_ID)
            val biometricId = prefs.getString(KEY_BIO_ID, null)
            if (biometricId != null && biometricId != profile.studentId) edit.remove(KEY_BIO_ID)
            check(edit.commit()) { "Cannot save demo session" }
            _session.value = StudentSession(
                isRestoring = false,
                profile = profile,
                remembered = remember && !BuildConfig.USE_SUPABASE,
                biometricStudentId = biometricId?.takeIf { !BuildConfig.USE_SUPABASE && it == profile.studentId },
            )
        }
    }

    override suspend fun updateProfile(profile: StudentProfile) {
        mutex.withLock {
            check(_session.value.profile?.studentId == profile.studentId) { "No active session" }
            _session.value = _session.value.copy(profile = profile)
        }
    }

    override suspend fun enableBiometric(studentId: String) {
        mutex.withLock {
            check(!BuildConfig.USE_SUPABASE) { "Biometric unlock requires an authenticated Supabase session" }
            check(_session.value.profile?.studentId == studentId) { "No active session" }
            check(prefs.edit().putString(KEY_BIO_ID, studentId).commit()) { "Cannot save biometric preference" }
            _session.value = _session.value.copy(biometricStudentId = studentId)
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
            check(!BuildConfig.USE_SUPABASE) { "Supabase sign-in requires credentials" }
            val studentId = _session.value.biometricStudentId ?: error("Biometric login is not enabled")
            val profile = auth.findStudent(studentId).getOrThrow() ?: error("Student account is missing")
            _session.value = _session.value.copy(isRestoring = false, profile = profile, failure = null)
        }
    }

    override suspend fun signOut() {
        mutex.withLock {
            auth.signOut()
            // Explicit log-out REVOKES device remembered sign-in and biometric opt-in.
            check(prefs.edit().remove(KEY_ID).remove(KEY_BIO_ID).commit()) { "Cannot revoke session" }
            _session.value = StudentSession(isRestoring = false)
        }
    }

    private companion object {
        const val KEY_ID = "remembered_student_id"
        const val KEY_BIO_ID = "biometric_student_id"
    }
}
