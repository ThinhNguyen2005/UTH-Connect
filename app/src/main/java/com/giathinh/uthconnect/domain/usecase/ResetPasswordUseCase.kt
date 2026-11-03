package com.giathinh.uthconnect.domain.usecase

import com.giathinh.uthconnect.domain.repository.AuthRepository
import com.giathinh.uthconnect.domain.validation.LoginValidator
import javax.inject.Inject

/** Firebase gửi liên kết đặt lại; không tiết lộ email có tồn tại hay không. */
class ResetPasswordUseCase @Inject constructor(private val auth: AuthRepository) {
    suspend operator fun invoke(email: String): Result<Unit> {
        val value = email.trim()
        require(value.contains('@') && LoginValidator().identifierError(value) == null)
        return auth.sendPasswordReset(value)
    }
}
