package com.giathinh.uthconnect.data.repository

import com.giathinh.uthconnect.domain.model.StudentProfile
import com.giathinh.uthconnect.domain.repository.AuthRepository
import com.giathinh.uthconnect.domain.repository.SessionFailure
import com.giathinh.uthconnect.domain.repository.SessionRepository
import com.giathinh.uthconnect.domain.repository.StudentSession
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Tùy chọn UID chỉ là lựa chọn UX; Firebase mới là bằng chứng xác thực. */
@Singleton
class PreferencesSessionRepository @Inject constructor(
    private val auth: AuthRepository,
    private val preferences: SessionPreferences,
) : SessionRepository {
    private val mutex = Mutex()
    private val current = MutableStateFlow(StudentSession())
    override val session = current.asStateFlow()
    private var restored = false

    override suspend fun restore() {
        mutex.withLock {
            if (restored) return
            current.value = current.value.copy(isRestoring = true, failure = null)
            try {
                val grants = preferences.read()
                if (grants.rememberedId == null && grants.biometricId == null) {
                    val identity = auth.currentIdentity().getOrThrow()
                    if (identity == null) auth.signOut()
                    else if (identity.emailVerified) {
                        val pending = auth.observePendingIdentity().first()
                        if (pending == null) auth.signOut()
                    }
                    current.value = StudentSession(isRestoring = false)
                } else {
                    val profile = auth.restoreAuthenticatedProfile().getOrThrow()
                    val valid = profile != null && profile.sessionIdentity == (grants.biometricId ?: grants.rememberedId)
                    if (!valid) {
                        auth.signOut()
                        preferences.write(SessionGrants())
                        current.value = StudentSession(isRestoring = false)
                    } else {
                        current.value = StudentSession(isRestoring = false,
                            profile = if (grants.biometricId == null) profile else null,
                            remembered = grants.rememberedId != null, biometricStudentId = grants.biometricId)
                    }
                }
                restored = true
            } catch (cancelled: CancellationException) {
                restored = false
                current.value = current.value.copy(isRestoring = false)
                throw cancelled
            } catch (_: Exception) {
                restored = false
                current.value = StudentSession(isRestoring = false, failure = SessionFailure.RestoreFailed)
            }
        }
    }

    override suspend fun refresh() {
        mutex.withLock {
            if (!restored || current.value.profile == null) return
            try {
                val profile = auth.restoreAuthenticatedProfile().getOrThrow()
                current.value = if (profile?.sessionIdentity == current.value.profile?.sessionIdentity)
                    current.value.copy(profile = profile, failure = null)
                else StudentSession(isRestoring = false)
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) {
                restored = false
                current.value = StudentSession(isRestoring = false, failure = SessionFailure.RestoreFailed)
            }
        }
    }

    override suspend fun monitorAuthChanges() = kotlinx.coroutines.coroutineScope {
        launch {
            auth.authChanges.collect { signedIn ->
                if (!signedIn) mutex.withLock {
                    if (current.value.profile != null || current.value.biometricStudentId != null) {
                        current.value = StudentSession(isRestoring = false)
                        restored = false
                    }
                }
            }
        }
        auth.observeProfileChanges().collect { profile ->
            mutex.withLock {
                val active = current.value.profile
                // Token refresh không được tự mở khóa cổng biometric ở cold start.
                if (active != null) {
                    if (profile != null && profile.authUid == active.authUid) current.value = current.value.copy(profile = profile)
                    else {
                        current.value = StudentSession(isRestoring = false, failure = SessionFailure.RestoreFailed)
                        restored = false
                    }
                }
            }
        }
    }

    override suspend fun start(profile: StudentProfile, remember: Boolean) {
        mutex.withLock {
            val identity = profile.sessionIdentity
            val bio = preferences.read().biometricId?.takeIf { it == identity }
            try { preferences.write(SessionGrants(if (remember) identity else null, bio)) }
            catch (error: Exception) { auth.signOut(); throw error }
            restored = true
            current.value = StudentSession(isRestoring = false, profile = profile,
                remembered = remember, biometricStudentId = bio)
        }
    }
    override suspend fun updateProfile(profile: StudentProfile) {
        mutex.withLock {
            check(current.value.profile?.sessionIdentity == profile.sessionIdentity) { "Không có phiên phù hợp" }
            current.value = current.value.copy(profile = profile)
        }
    }
    override suspend fun enableBiometric(studentId: String) {
        mutex.withLock {
            val profile = current.value.profile ?: error("Chưa đăng nhập")
            check(profile.studentId == studentId)
            preferences.write(preferences.read().copy(biometricId = profile.sessionIdentity))
            current.value = current.value.copy(biometricStudentId = profile.sessionIdentity)
        }
    }
    override suspend fun disableBiometric() {
        mutex.withLock {
            preferences.write(preferences.read().copy(biometricId = null))
            current.value = current.value.copy(biometricStudentId = null)
        }
    }
    override suspend fun unlockWithBiometric() {
        mutex.withLock {
            val id = current.value.biometricStudentId ?: error("Chưa bật sinh trắc học")
            val profile = auth.restoreAuthenticatedProfile().getOrThrow() ?: error("Phiên đã hết hạn")
            check(profile.sessionIdentity == id) { "Tài khoản đã thay đổi" }
            current.value = current.value.copy(profile = profile, failure = null)
        }
    }
    override suspend fun signOut() = kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) {
        mutex.withLock {
            auth.signOut()
            // Đĩa lỗi vẫn phải thu hồi credential Firebase và profile trong bộ nhớ.
            try { preferences.write(SessionGrants()) }
            finally { current.value = StudentSession(isRestoring = false); restored = true }
        }
    }
}
