package com.giathinh.uthconnect.domain.validation

/** Input constraints specified by the assignment: nonblank MSSV / @ut.edu.vn email. */
class LoginValidator {
    private val mssvPattern = Regex("^[0-9]{8,12}$")
    private val schoolEmailPattern = Regex(
        "^[A-Za-z0-9._%+\\-]+@ut\\.edu\\.vn$",
        RegexOption.IGNORE_CASE,
    )

    fun validateIdentifier(raw: String): InputError? {
        val value = raw.trim()
        return when {
            value.isEmpty() -> InputError.REQUIRED
            mssvPattern.matches(value) || schoolEmailPattern.matches(value) -> null
            else -> InputError.INVALID_IDENTIFIER
        }
    }

    fun validatePassword(password: String): InputError? =
        if (password.isBlank()) InputError.REQUIRED else null
}

enum class InputError {
    REQUIRED,
    INVALID_IDENTIFIER,
}
