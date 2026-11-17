package com.giathinh.uthconnect.domain.usecase

import com.giathinh.uthconnect.domain.repository.AuthRepository
import javax.inject.Inject

sealed interface CompletePasswordResetOutcome {
    data object Success : CompletePasswordResetOutcome
    data class InvalidForm(val passwordError: String?, val confirmError: String?) : CompletePasswordResetOutcome
    data object TokenExpired : CompletePasswordResetOutcome
    data class Failure(val message: String) : CompletePasswordResetOutcome
}

class CompletePasswordResetUseCase @Inject constructor(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(
        email: String,
        otp: String,
        password: String,
        confirmPassword: String,
        recoveryToken: String? = null,
    ): CompletePasswordResetOutcome {
        val formError = validateForm(password.trim(), confirmPassword.trim(), email.trim(), otp.trim(), recoveryToken)
        if (formError != null) return formError

        return authRepository.completePasswordReset(
            email = email.trim(),
            otp = otp.trim(),
            newPassword = password.trim(),
            recoveryToken = recoveryToken,
        ).fold(
            onSuccess = { CompletePasswordResetOutcome.Success },
            onFailure = { error ->
                val msg = error.message.orEmpty()
                if (msg.contains("expired", ignoreCase = true) || msg.contains("invalid", ignoreCase = true)) {
                    CompletePasswordResetOutcome.TokenExpired
                } else {
                    CompletePasswordResetOutcome.Failure(msg.ifEmpty { "Không thể đặt lại mật khẩu" })
                }
            },
        )
    }

    private fun validateForm(
        password: String,
        confirm: String,
        email: String,
        otp: String,
        token: String?,
    ): CompletePasswordResetOutcome? {
        val passwordError = when {
            password.isBlank() -> "Vui lòng nhập mật khẩu mới"

            !isStrongPassword(password) ->
                "Mật khẩu phải từ 8-64 ký tự, có chữ hoa, chữ thường, chữ số và ký tự đặc biệt"

            else -> null
        }
        val confirmError = when {
            confirm.isBlank() -> "Vui lòng nhập lại mật khẩu mới"
            password != confirm -> "Mật khẩu xác nhận không khớp"
            else -> null
        }
        return when {
            passwordError != null || confirmError != null ->
                CompletePasswordResetOutcome.InvalidForm(passwordError, confirmError)

            token.isNullOrBlank() && (!email.contains('@') || !otp.matches(Regex("[0-9]{6,8}"))) ->
                CompletePasswordResetOutcome.Failure("Vui lòng nhập mã OTP hợp lệ từ email sinh viên")

            else -> null
        }
    }

    private fun isStrongPassword(password: String): Boolean = password.length in 8..64 &&
        password.none(Char::isWhitespace) &&
        password.any(Char::isUpperCase) &&
        password.any(Char::isLowerCase) &&
        password.any(Char::isDigit) &&
        password.any { !it.isLetterOrDigit() && !it.isWhitespace() }
}
