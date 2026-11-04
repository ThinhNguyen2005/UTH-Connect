package com.giathinh.uthconnect.domain.repository

import com.giathinh.uthconnect.domain.model.StudentProfile

interface AuthRepository {
    suspend fun authenticate(identifier: String, password: String): Result<StudentProfile?>
    suspend fun findStudent(studentId: String): Result<StudentProfile?>
    /** False if old password does not match. Result.failure means storage failure. */
    suspend fun changePassword(studentId: String, oldPassword: String, newPassword: String): Result<Boolean>
    suspend fun signOut() {}

    /**
     * Redeems a Google ID token from Credential Manager for a Supabase session and returns the
     * verified student profile behind it.
     *
     * The failure is part of the contract, not noise:
     * [StudentVerificationRequiredException] means the identity exists but no administrator has
     * confirmed its MSSV, so every student RPC still refuses it — callers must show a pending
     * state instead of a generic error. Backends without Google support fail with
     * [GoogleSignInUnsupportedException].
     */
    suspend fun authenticateWithGoogle(idToken: String, rawNonce: String): Result<StudentProfile> =
        Result.failure(GoogleSignInUnsupportedException())
}

/** Thrown when a Google identity has no admin-verified MSSV yet. */
class StudentVerificationRequiredException(
    val email: String,
    val displayName: String,
) : IllegalStateException("MSSV chưa được quản trị viên xác minh")

/** Thrown by backends that do not offer Google sign-in (demo data, Firebase). */
class GoogleSignInUnsupportedException : IllegalStateException("Google Sign-In requires the Supabase backend")

/** Thrown when an account does not belong to the @ut.edu.vn student domain. */
class DomainRestrictionException(
    val email: String,
) : IllegalArgumentException("Chỉ chấp nhận tài khoản Google sinh viên (@ut.edu.vn)")

