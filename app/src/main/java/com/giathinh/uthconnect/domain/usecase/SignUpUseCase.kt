package com.giathinh.uthconnect.domain.usecase

import com.giathinh.uthconnect.domain.repository.AuthRepository
import com.giathinh.uthconnect.domain.validation.LoginValidator
import javax.inject.Inject

private const val MIN_STUDENT_ID_LENGTH = 8
private const val MAX_STUDENT_ID_LENGTH = 12
private const val MIN_SIGN_UP_PASSWORD_LENGTH = 6

sealed interface SignUpOutcome {
    data class Success(val needsEmailConfirmation: Boolean, val email: String) : SignUpOutcome
    data class InvalidForm(
        val nameError: String? = null,
        val emailError: String? = null,
        val passwordError: String? = null,
        val confirmError: String? = null,
    ) : SignUpOutcome
    data class DomainRestriction(val email: String) : SignUpOutcome
    data class Failure(val message: String) : SignUpOutcome
}

class SignUpUseCase @Inject constructor(
    private val authRepository: AuthRepository,
) {
    private val validator = LoginValidator()

    suspend operator fun invoke(
        identifier: String,
        password: String,
        confirmPassword: String,
        displayName: String,
        faculty: String,
    ): SignUpOutcome {
        val trimmedName = displayName.trim()
        val trimmedIdentifier = identifier.trim()
        val trimmedPassword = password.trim()
        val trimmedConfirm = confirmPassword.trim()
        val trimmedFaculty = faculty.trim().ifEmpty { "Chưa cập nhật" }

        val email = studentEmail(trimmedIdentifier)

        var nameError: String? = null
        var emailError: String? = null
        var passwordError: String? = null
        var confirmError: String? = null

        if (trimmedName.isBlank()) {
            nameError = "Vui lòng nhập họ và tên"
        }
        if (!validator.isUniversityEmail(email)) {
            emailError = "Chỉ chấp nhận email sinh viên (@ut.edu.vn hoặc @uth.edu.vn)"
        }
        if (trimmedPassword.length < MIN_SIGN_UP_PASSWORD_LENGTH) {
            passwordError = "Mật khẩu phải có ít nhất 6 ký tự"
        }
        if (trimmedPassword != trimmedConfirm) {
            confirmError = "Mật khẩu xác nhận không khớp"
        }

        val hasIdentityError = nameError != null || emailError != null
        val hasPasswordError = passwordError != null || confirmError != null
        if (hasIdentityError || hasPasswordError) {
            return SignUpOutcome.InvalidForm(
                nameError = nameError,
                emailError = emailError,
                passwordError = passwordError,
                confirmError = confirmError,
            )
        }

        return authRepository.signUp(
            email = email,
            password = trimmedPassword,
            displayName = trimmedName,
            faculty = trimmedFaculty,
        ).fold(
            onSuccess = { result ->
                SignUpOutcome.Success(
                    needsEmailConfirmation = result.needsEmailConfirmation,
                    email = result.email,
                )
            },
            onFailure = { error ->
                if (error.message?.contains("DOMAIN_RESTRICTION", ignoreCase = true) == true) {
                    SignUpOutcome.DomainRestriction(email)
                } else {
                    SignUpOutcome.Failure(error.message ?: "Đăng ký thất bại")
                }
            },
        )
    }

    private fun studentEmail(identifier: String): String {
        val isStudentId = '@' !in identifier && identifier.all { it.isDigit() }
        return if (isStudentId && identifier.length in MIN_STUDENT_ID_LENGTH..MAX_STUDENT_ID_LENGTH) {
            "$identifier@ut.edu.vn"
        } else {
            identifier
        }
    }
}
