package com.giathinh.uthconnect.domain.validation

enum class PasswordError { Required, Weak, SameAsOld, ConfirmationMismatch }

data class PasswordValidation(
    val oldError: PasswordError? = null,
    val newError: PasswordError? = null,
    val confirmationError: PasswordError? = null,
) {
    val valid: Boolean get() = oldError == null && newError == null && confirmationError == null
}

/** 8..64 characters, uppercase, lowercase, decimal digit, special character, no whitespace. */
class PasswordChangeValidator {
    fun validate(old: String, updated: String, confirmation: String): PasswordValidation {
        val strong = updated.length in 8..64 && updated.none(Char::isWhitespace) &&
            updated.any(Char::isUpperCase) && updated.any(Char::isLowerCase) &&
            updated.any(Char::isDigit) && updated.any { !it.isLetterOrDigit() && !it.isWhitespace() }
        return PasswordValidation(
            oldError = if (old.isBlank()) PasswordError.Required else null,
            newError = when {
                updated.isBlank() -> PasswordError.Required
                old == updated -> PasswordError.SameAsOld
                !strong -> PasswordError.Weak
                else -> null
            },
            confirmationError = when {
                confirmation.isEmpty() -> PasswordError.Required
                confirmation != updated -> PasswordError.ConfirmationMismatch
                else -> null
            },
        )
    }
}
