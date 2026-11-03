package com.giathinh.uthconnect.presentation.auth

import com.giathinh.uthconnect.domain.model.AuthIdentity
import com.giathinh.uthconnect.domain.model.StudentProfile
import com.giathinh.uthconnect.domain.repository.AuthError
import com.giathinh.uthconnect.domain.repository.AuthException
import com.giathinh.uthconnect.domain.repository.AuthRepository
import com.giathinh.uthconnect.domain.repository.SessionRepository
import com.giathinh.uthconnect.domain.repository.StudentSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.CancellationException

internal class Auth3FakeRepository : AuthRepository {
    val pending = MutableStateFlow<AuthIdentity?>(null)
    var error: AuthError? = null
    var cancels = false
    var registerCalls = 0
    var sends = 0
    var resets = 0
    var profile = StudentProfile("", "student@ut.edu.vn", "Test Student", "", "", "", uthVerified = true, authUid = "uid-test")
    override fun observePendingIdentity() = pending
    override suspend fun currentIdentity() = Result.success(pending.value)
    private fun failure(): AuthException? { if (cancels) throw CancellationException("test cancel"); return error?.let(::AuthException) }
    override suspend fun authenticate(identifier: String, password: String): Result<StudentProfile?> =
        failure()?.let { Result.failure(it) } ?: Result.success(profile)
    override suspend fun register(email: String, password: String): Result<AuthIdentity> {
        registerCalls++
        failure()?.let { return Result.failure(it) }
        val identity = AuthIdentity("uid-test", email, false)
        pending.value = identity
        return Result.success(identity)
    }
    override suspend fun signInWithGoogle(idToken: String): Result<StudentProfile?> =
        failure()?.let { Result.failure(it) } ?: Result.success(profile)
    override suspend fun sendVerification(): Result<Unit> {
        sends++
        return failure()?.let { Result.failure(it) } ?: Result.success(Unit)
    }
    override suspend fun checkVerification(): Result<StudentProfile> {
        failure()?.let { return Result.failure(it) }
        pending.value = null
        return Result.success(profile)
    }
    override suspend fun sendPasswordReset(email: String): Result<Unit> {
        resets++
        return failure()?.let { Result.failure(it) } ?: Result.success(Unit)
    }
    override suspend fun signOut() { pending.value = null }
    override suspend fun findStudent(studentId: String) = Result.success<StudentProfile?>(profile)
    override suspend fun changePassword(studentId: String, oldPassword: String, newPassword: String) = Result.success(true)
}

internal class Auth3FakeSession(private val auth: Auth3FakeRepository) : SessionRepository {
    var startError: Exception? = null
    override val session = MutableStateFlow(StudentSession(isRestoring = false))
    override suspend fun restore() = Unit
    override suspend fun start(profile: StudentProfile, remember: Boolean) { startError?.let { throw it }; session.value = StudentSession(isRestoring = false, profile = profile, remembered = remember) }
    override suspend fun updateProfile(profile: StudentProfile) { session.value = session.value.copy(profile = profile) }
    override suspend fun enableBiometric(studentId: String) = Unit
    override suspend fun disableBiometric() = Unit
    override suspend fun unlockWithBiometric() = Unit
    override suspend fun signOut() { auth.signOut(); session.value = StudentSession(isRestoring = false) }
}
