package com.giathinh.uthconnect.data.repository

import com.giathinh.uthconnect.domain.model.StudentProfile
import com.giathinh.uthconnect.domain.model.UserRole
import com.giathinh.uthconnect.domain.repository.AuthError
import com.giathinh.uthconnect.domain.repository.AuthException
import com.giathinh.uthconnect.domain.repository.AuthRepository
import com.giathinh.uthconnect.domain.repository.StudentContactRepository
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.firestore.FirebaseFirestoreException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Chỉ tin claims của ID token; dữ liệu role/verified trong users bị bỏ qua. */
@Singleton
class FirebaseAuthRepository @Inject constructor(private val source: FirebaseAuthSource) :
    AuthRepository, StudentContactRepository {
    private val mutex = Mutex()
    override val authChanges get() = source.changes

    override suspend fun authenticate(identifier: String, password: String): Result<StudentProfile?> = guarded {
        mutex.withLock {
            // Chưa có hợp đồng backend ánh xạ MSSV; không suy đoán email của sinh viên.
            if (!identifier.contains('@')) throw AuthException(AuthError.StudentIdUnavailable)
            try {
                source.signIn(identifier.trim(), password)
                verifiedProfile() ?: throw AuthException(AuthError.InvalidSession)
            } catch (cancelled: CancellationException) {
                source.signOut()
                throw cancelled
            } catch (error: Exception) {
                source.signOut()
                if (mapError(error).reason == AuthError.InvalidCredentials ||
                    (error is FirebaseAuthException && error.errorCode == "ERROR_USER_NOT_FOUND")) null else throw error
            }
        }
    }

    override suspend fun restoreAuthenticatedProfile(): Result<StudentProfile?> = guarded {
        mutex.withLock {
            try { verifiedProfile() }
            catch (error: Exception) {
                if (error is CancellationException) throw error
                val mapped = mapError(error)
                if (mapped.reason in setOf(AuthError.Disabled, AuthError.InvalidSession,
                        AuthError.VerificationRequired, AuthError.ApprovalRequired, AuthError.ProfileMissing)) {
                    source.signOut()
                }
                throw mapped
            }
        }
    }

    override suspend fun findStudent(studentId: String): Result<StudentProfile?> = guarded {
        mutex.withLock { verifiedProfile()?.takeIf { it.studentId == studentId } }
    }
    override suspend fun signOut() { mutex.withLock { source.signOut() } }
    override suspend fun sendPasswordReset(email: String): Result<Unit> = guarded {
        try { source.resetPassword(email) }
        catch (error: FirebaseAuthException) {
            if (error.errorCode != "ERROR_USER_NOT_FOUND") throw error
        }
    }
    override suspend fun changePassword(studentId: String, oldPassword: String, newPassword: String): Result<Boolean> = guarded {
        mutex.withLock {
            check(verifiedProfile()?.studentId == studentId)
            try { source.changePassword(oldPassword, newPassword); true }
            catch (error: Exception) {
                if (error is CancellationException) throw error
                if (mapError(error).reason == AuthError.InvalidCredentials) false else throw error
            }
        }
    }
    override suspend fun updateContact(studentId: String, phone: String, personalEmail: String,
        currentAddress: String): Result<StudentProfile> = guarded {
        mutex.withLock {
            val profile = verifiedProfile() ?: throw AuthException(AuthError.InvalidSession)
            check(profile.studentId == studentId)
            source.updateContact(mapOf("phoneNumber" to phone, "personalEmail" to personalEmail,
                "currentAddress" to currentAddress))
            profile.copy(phoneNumber = phone, personalEmail = personalEmail, currentAddress = currentAddress)
        }
    }

    private suspend fun verifiedProfile(): StudentProfile? {
        val account = source.account() ?: return null
        if (!account.emailVerified) {
            source.sendVerification()
            throw AuthException(AuthError.VerificationRequired)
        }
        if (account.claims["uthVerified"] != true) throw AuthException(AuthError.ApprovalRequired)
        fun field(name: String) = account.fields[name] as? String ?: ""
        val id = field("studentId")
        if (id.isBlank() || field("displayName").isBlank()) throw AuthException(AuthError.ProfileMissing)
        val role = when (account.claims["role"]) {
            "organizer", "ORGANIZER" -> UserRole.ORGANIZER
            "admin", "ADMIN" -> UserRole.ADMIN
            else -> UserRole.STUDENT
        }
        return StudentProfile(id, account.email, field("displayName"), field("className"), field("major"),
            field("faculty"), field("avatarUrl").ifBlank { null }, field("phoneNumber"), field("personalEmail"),
            field("currentAddress"), role, uthVerified = true, authUid = account.uid)
    }

    private suspend fun <T> guarded(block: suspend () -> T): Result<T> = try { Result.success(block()) }
    catch (cancelled: CancellationException) { throw cancelled }
    catch (error: Exception) { Result.failure(mapError(error)) }

    private fun mapError(error: Exception): AuthException = when (error) {
        is AuthException -> error
        is FirebaseNetworkException -> AuthException(AuthError.Network)
        is FirebaseTooManyRequestsException -> AuthException(AuthError.TooManyRequests)
        is FirebaseAuthException -> AuthException(when (error.errorCode) {
            in INVALID_CREDENTIALS -> AuthError.InvalidCredentials
            "ERROR_USER_DISABLED" -> AuthError.Disabled
            "ERROR_USER_NOT_FOUND", "ERROR_USER_TOKEN_EXPIRED", "ERROR_INVALID_USER_TOKEN" -> AuthError.InvalidSession
            "ERROR_OPERATION_NOT_ALLOWED", "ERROR_INVALID_API_KEY" -> AuthError.Configuration
            else -> AuthError.Unexpected
        })
        is FirebaseFirestoreException -> AuthException(when (error.code) {
            FirebaseFirestoreException.Code.UNAVAILABLE, FirebaseFirestoreException.Code.DEADLINE_EXCEEDED -> AuthError.Network
            FirebaseFirestoreException.Code.PERMISSION_DENIED -> AuthError.PermissionDenied
            FirebaseFirestoreException.Code.UNAUTHENTICATED -> AuthError.InvalidSession
            else -> AuthError.Unexpected
        })
        else -> AuthException(AuthError.Unexpected)
    }

    private companion object {
        val INVALID_CREDENTIALS = setOf("ERROR_WRONG_PASSWORD", "ERROR_INVALID_CREDENTIAL", "ERROR_INVALID_LOGIN_CREDENTIALS")
    }
}
