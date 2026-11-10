package com.giathinh.uthconnect.domain.repository

import com.giathinh.uthconnect.domain.model.StudentProfile

interface AuthRepository {
    suspend fun authenticate(identifier: String, password: String): Result<StudentProfile?>
    suspend fun findStudent(studentId: String): Result<StudentProfile?>

    /** False if old password does not match. Result.failure means storage failure. */
    suspend fun changePassword(studentId: String, oldPassword: String, newPassword: String): Result<Boolean>
    suspend fun signOut() {}

    /**
     * Registers a new student account.
     */
    suspend fun signUp(
        email: String,
        password: String,
        displayName: String,
        faculty: String,
    ): Result<SignUpResult> = Result.failure(UnsupportedOperationException("signUp not implemented"))

    /**
     * Sends password recovery instructions to the student email.
     */
    suspend fun sendPasswordReset(email: String): Result<Unit> = Result.failure(UnsupportedOperationException("sendPasswordReset not implemented"))

    /** Verify a recovery OTP with the provider before changing a password. */
    suspend fun completePasswordReset(email: String, otp: String, newPassword: String): Result<Unit> = Result.failure(UnsupportedOperationException("completePasswordReset not implemented"))

    /**
     * Submits a student ID verification request.
     */
    suspend fun submitVerificationRequest(studentId: String): Result<VerificationRequestResult> = Result.failure(UnsupportedOperationException("submitVerificationRequest not implemented"))

    /**
     * Checks the latest student ID verification request status.
     */
    suspend fun checkVerificationStatus(): Result<VerificationStatus?> = Result.failure(UnsupportedOperationException("checkVerificationStatus not implemented"))

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
    suspend fun authenticateWithGoogle(idToken: String, rawNonce: String): Result<StudentProfile> = Result.failure(GoogleSignInUnsupportedException())
}

data class SignUpResult(val needsEmailConfirmation: Boolean, val email: String)
enum class VerificationStatus { PENDING, APPROVED, REJECTED }
data class VerificationRequestResult(val requestId: String, val status: VerificationStatus)

/** Thrown when a Google identity has no admin-verified MSSV yet. */
class StudentVerificationRequiredException(
    val email: String,
    val displayName: String,
) : IllegalStateException("MSSV chưa được quản trị viên xác minh")

/** Thrown by backends that do not offer Google sign-in (demo data, Firebase). */
class GoogleSignInUnsupportedException : IllegalStateException("Google Sign-In requires the Supabase backend")

/** Thrown when an account does not belong to the @ut.edu.vn or @uth.edu.vn student domain. */
class DomainRestrictionException(
    val email: String,
) : IllegalArgumentException("Chỉ chấp nhận tài khoản Google sinh viên (@ut.edu.vn hoặc @uth.edu.vn)")
