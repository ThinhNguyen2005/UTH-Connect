package com.giathinh.uthconnect.domain.repository

import com.giathinh.uthconnect.domain.model.StudentProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

interface AuthRepository {
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
}

class AuthException(val reason: AuthError) : Exception(reason.name)
