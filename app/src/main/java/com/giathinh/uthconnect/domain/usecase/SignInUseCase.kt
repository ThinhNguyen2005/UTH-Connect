package com.giathinh.uthconnect.domain.usecase

import com.giathinh.uthconnect.domain.model.StudentProfile
import com.giathinh.uthconnect.domain.repository.AuthRepository
import com.giathinh.uthconnect.domain.repository.SessionRepository
import com.giathinh.uthconnect.domain.validation.LoginFieldError
import com.giathinh.uthconnect.domain.validation.LoginValidator
import javax.inject.Inject

/** Kiểm tra dữ liệu trước khi gọi kho xác thực. */
class SignInUseCase @Inject constructor(
    private val auth: AuthRepository,
    private val session: SessionRepository,
) {
    private val validator = LoginValidator()

    suspend operator fun invoke(identifier: String, password: String, remember: Boolean): SignInResult {
        val idError = validator.identifierError(identifier)
        val passwordError = validator.passwordError(password)
        if (idError != null || passwordError != null) {
            return SignInResult.InvalidForm(idError, passwordError)
        }
        return auth.authenticate(identifier.trim(), password).fold(
            onSuccess = { student ->
                if (student == null) SignInResult.InvalidCredentials
                else {
                    session.start(student, remember)
                    SignInResult.Success(student)
                }
            },
            onFailure = { SignInResult.Failure },
        )
    }
}

sealed interface SignInResult {
    data class Success(val student: StudentProfile) : SignInResult
    data class InvalidForm(
        val identifier: LoginFieldError?,
        val password: LoginFieldError?,
    ) : SignInResult
    data object InvalidCredentials : SignInResult
    data object Failure : SignInResult
}
