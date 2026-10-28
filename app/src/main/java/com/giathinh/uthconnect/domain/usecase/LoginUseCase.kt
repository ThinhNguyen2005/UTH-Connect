package com.giathinh.uthconnect.domain.usecase

import com.giathinh.uthconnect.domain.model.StudentProfile
import com.giathinh.uthconnect.domain.repository.AuthRepository
import com.giathinh.uthconnect.domain.validation.InputError
import com.giathinh.uthconnect.domain.validation.LoginValidator

/** Entire login algorithm, independent from Android and Compose. */
class LoginUseCase(
    private val repository: AuthRepository,
    private val validator: LoginValidator = LoginValidator(),
) {
    suspend operator fun invoke(identifier: String, password: String): LoginResult {
        val idError = validator.validateIdentifier(identifier)
        val passwordError = validator.validatePassword(password)
        if (idError != null || passwordError != null) {
            return LoginResult.InvalidForm(idError, passwordError)
        }

        val profile = repository.authenticate(identifier.trim(), password)
        return if (profile != null) {
            LoginResult.Success(profile)
        } else {
            LoginResult.InvalidCredentials
        }
    }
}

sealed interface LoginResult {
    data class Success(val profile: StudentProfile) : LoginResult
    data class InvalidForm(
        val identifierError: InputError?,
        val passwordError: InputError?,
    ) : LoginResult
    data object InvalidCredentials : LoginResult
}
