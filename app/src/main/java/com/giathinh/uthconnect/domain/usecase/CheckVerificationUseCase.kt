package com.giathinh.uthconnect.domain.usecase

import com.giathinh.uthconnect.domain.model.StudentProfile
import com.giathinh.uthconnect.domain.repository.AuthRepository
import com.giathinh.uthconnect.domain.repository.SessionRepository
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

class CheckVerificationUseCase @Inject constructor(private val auth: AuthRepository, private val sessions: SessionRepository) {
    suspend operator fun invoke(remember: Boolean): Result<StudentProfile> = auth.checkVerification().fold(
        onSuccess = { profile ->
            try {
                sessions.start(profile, remember)
                Result.success(profile)
            } catch (cancelled: CancellationException) {
                withContext(NonCancellable) { auth.signOut() }
                throw cancelled
            } catch (error: Exception) {
                withContext(NonCancellable) { auth.signOut() }
                Result.failure(error)
            }
        },
        onFailure = { Result.failure(it) },
    )
}
