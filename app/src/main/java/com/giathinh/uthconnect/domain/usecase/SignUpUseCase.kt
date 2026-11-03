package com.giathinh.uthconnect.domain.usecase

import com.giathinh.uthconnect.domain.model.AuthIdentity
import com.giathinh.uthconnect.domain.repository.AuthError
import com.giathinh.uthconnect.domain.repository.AuthException
import com.giathinh.uthconnect.domain.repository.AuthRepository
import com.giathinh.uthconnect.domain.validation.LoginValidator
import javax.inject.Inject

class SignUpUseCase @Inject constructor(private val auth: AuthRepository) {
    suspend operator fun invoke(email: String, password: String): Result<AuthIdentity> {
        val validator = LoginValidator()
        if (validator.emailError(email) != null) return Result.failure(AuthException(AuthError.DomainNotAllowed))
        if (validator.signUpPasswordError(password) != null) return Result.failure(AuthException(AuthError.WeakPassword))
        return auth.register(validator.normalizeEmail(email), password)
    }
}
