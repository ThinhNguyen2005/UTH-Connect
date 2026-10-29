package com.giathinh.uthconnect.domain.validation

/** Phạm vi bài tập: MSSV 8–12 chữ số hoặc email @ut.edu.vn. */
class LoginValidator {
    private val studentIdPattern = Regex("^[0-9]{8,12}$")
    private val universityEmailPattern = Regex("^[A-Za-z0-9._%+\\-]+@ut\\.edu\\.vn$", RegexOption.IGNORE_CASE)

    fun identifierError(raw: String): LoginFieldError? {
        val input = raw.trim()
        return when {
            input.isEmpty() -> LoginFieldError.Required
            studentIdPattern.matches(input) || universityEmailPattern.matches(input) -> null
            else -> LoginFieldError.InvalidIdentifier
        }
    }

    fun passwordError(password: String): LoginFieldError? =
        if (password.isBlank()) LoginFieldError.Required else null
}

enum class LoginFieldError { Required, InvalidIdentifier }
