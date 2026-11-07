package com.giathinh.uthconnect.domain.usecase

import com.giathinh.uthconnect.domain.model.StudentProfile
import com.giathinh.uthconnect.domain.repository.AuthRepository
import com.giathinh.uthconnect.domain.repository.GoogleSignInUnsupportedException
import com.giathinh.uthconnect.domain.repository.SessionRepository
import com.giathinh.uthconnect.domain.repository.StudentVerificationRequiredException
import kotlinx.coroutines.CancellationException
import javax.inject.Inject

/**
 * Google counterpart of [SignInUseCase]: redeems an ID token, then opens the local student
 * session. Never called with a password, so there is nothing to validate first.
 *
 * The UI chooses whether a verified Google session should be remembered on this device.
 * Only the rotated refresh token is persisted by the Android Keystore-backed session store.
 */
class SignInWithGoogleUseCase @Inject constructor(
    private val auth: AuthRepository,
    private val session: SessionRepository,
) {
    suspend operator fun invoke(idToken: String, rawNonce: String, remember: Boolean = false): SignInWithGoogleResult = try {
        val profile = auth.authenticateWithGoogle(idToken, rawNonce).getOrElse { error ->
            return when (error) {
                is StudentVerificationRequiredException ->
                    SignInWithGoogleResult.VerificationPending(error.email, error.displayName)

                is GoogleSignInUnsupportedException -> SignInWithGoogleResult.Unsupported

                else -> SignInWithGoogleResult.Rejected
            }
        }
        session.start(profile, remember = remember)
        SignInWithGoogleResult.Success(profile)
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        SignInWithGoogleResult.Rejected
    }
}

sealed interface SignInWithGoogleResult {
    data class Success(val student: StudentProfile) : SignInWithGoogleResult

    /** Signed in at Google, but an administrator has not confirmed the MSSV yet. */
    data class VerificationPending(val email: String, val displayName: String) : SignInWithGoogleResult
    data object Rejected : SignInWithGoogleResult
    data object Unsupported : SignInWithGoogleResult
}
