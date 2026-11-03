package com.giathinh.uthconnect.domain.repository

import com.giathinh.uthconnect.domain.model.StudentProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import com.giathinh.uthconnect.domain.model.AuthIdentity
import kotlinx.coroutines.flow.flowOf

interface AuthRepository {
    fun observeProfileChanges(): Flow<StudentProfile?> = emptyFlow()
    fun observePendingIdentity(): Flow<AuthIdentity?> = flowOf(null)
    suspend fun currentIdentity(): Result<AuthIdentity?> = Result.success(null)
    suspend fun register(email: String, password: String): Result<AuthIdentity> =
        Result.failure(AuthException(AuthError.Configuration))
    suspend fun signInWithGoogle(idToken: String): Result<StudentProfile?> =
        Result.failure(AuthException(AuthError.Configuration))
    suspend fun sendVerification(): Result<Unit> = Result.failure(AuthException(AuthError.Configuration))
    suspend fun checkVerification(): Result<StudentProfile> = Result.failure(AuthException(AuthError.Configuration))
    suspend fun refreshClaims(): Result<StudentProfile?> = restoreAuthenticatedProfile()
    /** Tín hiệu SDK; profile chỉ được cấp sau khi kiểm tra lại với máy chủ. */
    val authChanges: Flow<Boolean> get() = emptyFlow()
    suspend fun restoreAuthenticatedProfile(): Result<StudentProfile?> =
        Result.failure(AuthException(AuthError.Configuration))
    suspend fun signOut() = Unit
    suspend fun sendPasswordReset(email: String): Result<Unit> =
        Result.failure(AuthException(AuthError.Configuration))
    suspend fun authenticate(identifier: String, password: String): Result<StudentProfile?>
    suspend fun findStudent(studentId: String): Result<StudentProfile?>
    /** False if old password does not match. Result.failure means storage failure. */
    suspend fun changePassword(studentId: String, oldPassword: String, newPassword: String): Result<Boolean>
}

enum class AuthError {
    Configuration, Network, Disabled, InvalidCredentials, InvalidSession, VerificationRequired, ApprovalRequired,
    ProfileMissing, PermissionDenied, TooManyRequests, StudentIdUnavailable, Unexpected,
    EmailNotVerified, DomainNotAllowed, EmailAlreadyInUse, WeakPassword, AccountCollision, Unknown,
}

class AuthException(val reason: AuthError) : Exception(reason.name)
