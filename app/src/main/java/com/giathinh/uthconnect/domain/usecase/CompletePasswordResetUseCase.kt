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
    suspend operator fun invoke(email: String, otp: String, password: String, confirmPassword: String): CompletePasswordResetOutcome {
        val trimmedPassword = password.trim()
        val trimmedConfirm = confirmPassword.trim()

        var passwordError: String? = null
        var confirmError: String? = null

        val isStrong = trimmedPassword.length in 8..64 &&
            trimmedPassword.none(Char::isWhitespace) &&
            trimmedPassword.any(Char::isUpperCase) &&
            trimmedPassword.any(Char::isLowerCase) &&
            trimmedPassword.any(Char::isDigit) &&
            trimmedPassword.any { !it.isLetterOrDigit() && !it.isWhitespace() }

        if (trimmedPassword.isBlank()) {
            passwordError = "Vui lòng nhập mật khẩu mới"
        } else if (!isStrong) {
            passwordError = "Mật khẩu phải từ 8-64 ký tự, có chữ hoa, chữ thường, chữ số và ký tự đặc biệt"
        }

        if (trimmedConfirm.isBlank()) {
            confirmError = "Vui lòng nhập lại mật khẩu mới"
        } else if (trimmedPassword != trimmedConfirm) {
            confirmError = "Mật khẩu xác nhận không khớp"
        }

        if (passwordError != null || confirmError != null) {
            return CompletePasswordResetOutcome.InvalidForm(passwordError, confirmError)
        }

        if (!email.contains('@') || !otp.trim().matches(Regex("[0-9]{6,8}"))) {
            return CompletePasswordResetOutcome.Failure("Vui lòng nhập mã OTP hợp lệ từ email sinh viên")
        }

        return authRepository.completePasswordReset(email.trim(), otp.trim(), trimmedPassword).fold(
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
}
