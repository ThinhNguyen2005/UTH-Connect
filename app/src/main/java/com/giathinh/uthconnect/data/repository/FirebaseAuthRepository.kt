package com.giathinh.uthconnect.data.repository

import com.giathinh.uthconnect.domain.model.AuthIdentity
import com.giathinh.uthconnect.domain.validation.LoginValidator
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import com.google.firebase.functions.FirebaseFunctionsException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Chỉ tin claims của ID token; dữ liệu role/verified trong users bị bỏ qua. */
@Singleton
class FirebaseAuthRepository @Inject constructor(private val source: FirebaseAuthSource) :
    AuthRepository, StudentContactRepository {
    private val mutex = Mutex()
    private val pending = MutableStateFlow<AuthIdentity?>(null)
    private val validator = LoginValidator()
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    override fun observeProfileChanges() = source.tokenChanges.mapLatest {
        mutex.withLock {
            try {
                val account = source.tokenAccount()
                if (account == null) { pending.value = null; null }
                else {
                    ensureDomain(account.email)
                    if (!account.emailVerified || account.claims["uthVerified"] != true) {
                        pending.value = AuthIdentity(account.uid, validator.normalizeEmail(account.email), account.emailVerified)
                        null
                    } else mapProfile(account)
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { null }
        }
    }.distinctUntilChanged()

    override fun observePendingIdentity() = pending.asStateFlow()

    override suspend fun currentIdentity(): Result<AuthIdentity?> = guarded {
        source.identity()?.let { account ->
            if (validator.emailError(account.email) != null) {
                source.signOut()
                pending.value = null
                throw AuthException(AuthError.DomainNotAllowed)
            }
            val identity = AuthIdentity(account.uid, validator.normalizeEmail(account.email), account.emailVerified)
            if (!account.emailVerified || account.claims["uthVerified"] != true) pending.value = identity
            identity
        } ?: run { pending.value = null; null }
    }

    override suspend fun register(email: String, password: String): Result<AuthIdentity> = guarded {
        mutex.withLock {
            ensureDomain(email)
            if (validator.signUpPasswordError(password) != null) throw AuthException(AuthError.WeakPassword)
            try {
                source.register(validator.normalizeEmail(email), password)
                val account = source.identity() ?: throw AuthException(AuthError.InvalidSession)
                ensureDomain(account.email)
                val identity = AuthIdentity(account.uid, validator.normalizeEmail(account.email), account.emailVerified)
                pending.value = identity
                source.sendVerification()
                identity
            } catch (cancelled: CancellationException) { source.signOut(); pending.value = null; throw cancelled }
        }
    }

    override suspend fun signInWithGoogle(idToken: String): Result<StudentProfile?> = guarded {
        mutex.withLock {
            try { source.signInGoogle(idToken); verifiedProfile() }
            catch (cancelled: CancellationException) { source.signOut(); pending.value = null; throw cancelled }
            catch (error: Exception) {
                if (mapError(error).reason !in PENDING_ERRORS) { source.signOut(); pending.value = null }
                throw error
            }
        }
    }

    override suspend fun sendVerification(): Result<Unit> = guarded {
        mutex.withLock {
            val account = source.identity() ?: throw AuthException(AuthError.InvalidSession)
            ensureDomain(account.email)
            if (!account.emailVerified) source.sendVerification()
        }
    }

    override suspend fun checkVerification(): Result<StudentProfile> = guarded {
        mutex.withLock {
            val identity = source.identity() ?: throw AuthException(AuthError.InvalidSession)
            ensureDomain(identity.email)
            if (!identity.emailVerified) throw AuthException(AuthError.EmailNotVerified)
            if (identity.claims["uthVerified"] == false) throw AuthException(AuthError.PermissionDenied)
            source.verifyUthAccount()
            verifiedProfile() ?: throw AuthException(AuthError.InvalidSession)
        }
    }

    private fun ensureDomain(email: String) {
        if (validator.emailError(email) != null) throw AuthException(AuthError.DomainNotAllowed)
    }
    override val authChanges get() = source.changes

    override suspend fun authenticate(identifier: String, password: String): Result<StudentProfile?> = guarded {
        mutex.withLock {
            // Chưa có hợp đồng backend ánh xạ MSSV; không suy đoán email của sinh viên.
            if (!identifier.contains('@')) throw AuthException(AuthError.StudentIdUnavailable)
            try {
                ensureDomain(identifier)
                source.signIn(validator.normalizeEmail(identifier), password)
                verifiedProfile() ?: throw AuthException(AuthError.InvalidSession)
            } catch (cancelled: CancellationException) {
                source.signOut()
                throw cancelled
            } catch (error: Exception) {
                if (mapError(error).reason !in PENDING_ERRORS) { source.signOut(); pending.value = null }
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
                        AuthError.ProfileMissing)) {
                    source.signOut()
                }
                throw mapped
            }
        }
    }

    override suspend fun findStudent(studentId: String): Result<StudentProfile?> = guarded {
        mutex.withLock { verifiedProfile()?.takeIf { it.studentId == studentId } }
    }
    override suspend fun signOut() { mutex.withLock { source.signOut(); pending.value = null } }
    override suspend fun sendPasswordReset(email: String): Result<Unit> = guarded {
        ensureDomain(email)
        try { source.resetPassword(validator.normalizeEmail(email)) }
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
        var account = try { source.account() } catch (error: AuthException) {
            if (error.reason != AuthError.ProfileMissing) throw error
            source.verifyUthAccount()
            source.account()
        } ?: return null
        ensureDomain(account.email)
        if (!account.emailVerified) {
            pending.value = AuthIdentity(account.uid, validator.normalizeEmail(account.email), false)
            source.sendVerification()
            throw AuthException(AuthError.EmailNotVerified)
        }
        if (account.claims["uthVerified"] == false) throw AuthException(AuthError.PermissionDenied)
        if (account.claims["uthVerified"] != true) {
            pending.value = AuthIdentity(account.uid, validator.normalizeEmail(account.email), true)
            source.verifyUthAccount()
            account = source.account() ?: throw AuthException(AuthError.InvalidSession)
            ensureDomain(account.email)
        }
        if (!account.emailVerified || account.claims["uthVerified"] != true) throw AuthException(AuthError.ApprovalRequired)
        return mapProfile(account)
    }

    private fun mapProfile(account: FirebaseAccount): StudentProfile {
        fun field(name: String) = account.fields[name] as? String ?: ""
        if (field("displayName").isBlank()) throw AuthException(AuthError.ProfileMissing)
        val role = when ((account.claims["role"] as? String)?.uppercase()) {
            "STUDENT" -> UserRole.STUDENT
            "ORGANIZER" -> UserRole.ORGANIZER
            "ADMIN" -> UserRole.ADMIN
            else -> throw AuthException(AuthError.ApprovalRequired)
        }
        pending.value = null
        return StudentProfile(field("studentId"), validator.normalizeEmail(account.email), field("displayName"),
            field("className"), field("major"), field("faculty"), field("avatarUrl").ifBlank { null },
            field("phoneNumber"), field("personalEmail"), field("currentAddress"), role,
            uthVerified = true, authUid = account.uid)
    }

    private suspend fun <T> guarded(block: suspend () -> T): Result<T> = try { Result.success(block()) }
    catch (cancelled: CancellationException) { throw cancelled }
    catch (error: Exception) { Result.failure(mapError(error)) }

    private fun mapError(error: Exception): AuthException = when (error) {
        is AuthException -> error
        is java.io.IOException -> AuthException(AuthError.Network)
        is FirebaseNetworkException -> AuthException(AuthError.Network)
        is FirebaseTooManyRequestsException -> AuthException(AuthError.TooManyRequests)
        is FirebaseFunctionsException -> AuthException(when (error.code) {
            FirebaseFunctionsException.Code.UNAUTHENTICATED -> AuthError.InvalidSession
            FirebaseFunctionsException.Code.NOT_FOUND, FirebaseFunctionsException.Code.UNIMPLEMENTED -> AuthError.Configuration
            FirebaseFunctionsException.Code.UNAVAILABLE, FirebaseFunctionsException.Code.DEADLINE_EXCEEDED -> AuthError.Network
            FirebaseFunctionsException.Code.RESOURCE_EXHAUSTED -> AuthError.TooManyRequests
            else -> when ((error.details as? Map<*, *>)?.get("reason")) {
                "DomainNotAllowed" -> AuthError.DomainNotAllowed
                "EmailNotVerified" -> AuthError.EmailNotVerified
                "Configuration" -> AuthError.Configuration
                "InvalidSession" -> AuthError.InvalidSession
                "PermissionDenied" -> AuthError.PermissionDenied
                "TooManyRequests" -> AuthError.TooManyRequests
                else -> AuthError.Unknown
            }
        })
        is FirebaseAuthException -> AuthException(when (error.errorCode) {
            "ERROR_EMAIL_ALREADY_IN_USE" -> AuthError.EmailAlreadyInUse
            "ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL", "ERROR_CREDENTIAL_ALREADY_IN_USE" -> AuthError.AccountCollision
            "ERROR_WEAK_PASSWORD" -> AuthError.WeakPassword
            "ERROR_INVALID_EMAIL" -> AuthError.DomainNotAllowed
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
        val PENDING_ERRORS = setOf(AuthError.EmailNotVerified, AuthError.VerificationRequired, AuthError.ApprovalRequired,
            AuthError.Network, AuthError.Configuration)
        val INVALID_CREDENTIALS = setOf("ERROR_WRONG_PASSWORD", "ERROR_INVALID_CREDENTIAL", "ERROR_INVALID_LOGIN_CREDENTIALS")
    }
}
