package com.giathinh.uthconnect.domain.usecase

import com.giathinh.uthconnect.domain.model.StudentProfile
import com.giathinh.uthconnect.domain.repository.AuthError
import com.giathinh.uthconnect.domain.repository.AuthException
import com.giathinh.uthconnect.domain.repository.AuthRepository
import com.giathinh.uthconnect.domain.repository.SessionRepository
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

class SignInWithGoogleUseCase @Inject constructor(private val auth: AuthRepository, private val sessions: SessionRepository) {
    suspend operator fun invoke(token: String, remember: Boolean): Result<StudentProfile> {
        if (token.isBlank()) return Result.failure(AuthException(AuthError.InvalidCredentials))
        return auth.signInWithGoogle(token).fold(onSuccess = { profile ->
            if (profile == null) Result.failure(AuthException(AuthError.InvalidCredentials))
            else try { sessions.start(profile, remember); Result.success(profile) }
            catch (cancelled: CancellationException) {
                withContext(NonCancellable) { auth.signOut() }; throw cancelled
            } catch (_: Exception) { auth.signOut(); Result.failure(AuthException(AuthError.Unknown)) }
        }, onFailure = { Result.failure(it) })
    }
}
