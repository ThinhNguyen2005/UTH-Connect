package com.giathinh.uthconnect.domain.validation

/** Phạm vi bài tập: MSSV 8–12 chữ số hoặc email @ut.edu.vn. */
class LoginValidator {
    fun normalizeEmail(raw: String): String = raw.trim().lowercase()
    fun emailError(raw: String): LoginFieldError? {
        val value = normalizeEmail(raw)
        if (value.isEmpty()) return LoginFieldError.Required
        if (value.length > 254 || value.count { it == '@' } != 1) return LoginFieldError.InvalidIdentifier
        val local = value.substringBefore('@')
        if (local.isEmpty() || local.length > 64 || local.startsWith('.') || local.endsWith('.') || ".." in local ||
            !local.matches(Regex("[a-z0-9._%+\\-]+"))) return LoginFieldError.InvalidIdentifier
        return if (value.substringAfter('@') == "ut.edu.vn") null else LoginFieldError.DomainNotAllowed
    }
    fun signUpPasswordError(password: String): LoginFieldError? = when {
        password.isBlank() -> LoginFieldError.Required
        password.length !in 8..64 || password.any(Char::isWhitespace) ||
            !password.any(Char::isUpperCase) || !password.any(Char::isLowerCase) ||
            !password.any(Char::isDigit) || !password.any { !it.isLetterOrDigit() } -> LoginFieldError.WeakPassword
        else -> null
    }
    private val studentIdPattern = Regex("^[0-9]{8,12}$")
    private val universityEmailPattern = Regex("^[A-Za-z0-9._%+\\-]+@ut\\.edu\\.vn$", RegexOption.IGNORE_CASE)

    fun identifierError(raw: String): LoginFieldError? {
        val input = raw.trim()
        return when {
            input.isEmpty() -> LoginFieldError.Required
            studentIdPattern.matches(input) -> null
            universityEmailPattern.matches(input) && emailError(input) == null -> null
            else -> LoginFieldError.InvalidIdentifier
        }
    }

    fun passwordError(password: String): LoginFieldError? =
        when {
            password.isBlank() -> LoginFieldError.Required
            password.length > 128 -> LoginFieldError.WeakPassword
            else -> null
        }
}

enum class LoginFieldError { Required, InvalidIdentifier, DomainNotAllowed, WeakPassword }
