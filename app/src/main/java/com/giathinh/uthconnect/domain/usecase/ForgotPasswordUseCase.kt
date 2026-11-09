package com.giathinh.uthconnect.domain.usecase

import com.giathinh.uthconnect.domain.repository.AuthRepository
import com.giathinh.uthconnect.domain.validation.LoginValidator
import java.io.IOException
import javax.inject.Inject

sealed interface ForgotPasswordOutcome {
    data object Success : ForgotPasswordOutcome
    data class InvalidEmail(val message: String) : ForgotPasswordOutcome
    data object NetworkError : ForgotPasswordOutcome
    data object RateLimited : ForgotPasswordOutcome
    data class Failure(val message: String) : ForgotPasswordOutcome
}

class ForgotPasswordUseCase @Inject constructor(
    private val authRepository: AuthRepository,
) {
    private val validator = LoginValidator()

    suspend operator fun invoke(rawIdentifier: String): ForgotPasswordOutcome {
        val trimmed = rawIdentifier.trim()
        val email = if ('@' in trimmed) {
            trimmed
        } else if (trimmed.all { it.isDigit() } && trimmed.length in 8..12) {
            "$trimmed@ut.edu.vn"
        } else {
            trimmed
        }

        if (!validator.isUniversityEmail(email)) {
            return ForgotPasswordOutcome.InvalidEmail("Vui lòng nhập đúng email sinh viên (@ut.edu.vn hoặc @uth.edu.vn)")
        }

        return authRepository.sendPasswordReset(email).fold(
            onSuccess = { ForgotPasswordOutcome.Success },
            onFailure = { error ->
                when {
                    error is UnsupportedOperationException ->
                        ForgotPasswordOutcome.Failure("Backend does not support password recovery")

                    error is IOException || error.message?.contains("timeout", ignoreCase = true) == true ->
                        ForgotPasswordOutcome.NetworkError

                    error.message?.contains("429", ignoreCase = true) == true ||
                        error.message?.contains("rate limit", ignoreCase = true) == true ->
                        ForgotPasswordOutcome.RateLimited

                    error.message?.contains("user not found", ignoreCase = true) == true ->
                        ForgotPasswordOutcome.Success

                    else -> ForgotPasswordOutcome.Failure("Could not send password recovery email")
                }
            },
        )
    }
}
