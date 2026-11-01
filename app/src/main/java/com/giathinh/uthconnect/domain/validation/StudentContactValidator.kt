package com.giathinh.uthconnect.domain.validation

enum class ContactFieldError { InvalidPhone, InvalidEmail, AddressTooLong }

data class ContactValidation(
    val phoneError: ContactFieldError? = null,
    val emailError: ContactFieldError? = null,
    val addressError: ContactFieldError? = null,
) {
    val valid: Boolean get() = phoneError == null && emailError == null && addressError == null
}

class StudentContactValidator {
    private val phonePattern = Regex("(?:0[0-9]{9}|\\+84[0-9]{9})")
    private val emailPattern = Regex("[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]+@[A-Za-z0-9-]+(?:\\.[A-Za-z0-9-]+)+")

    fun validate(phone: String, email: String, address: String) = ContactValidation(
        phoneError = if (phone.isNotBlank() && !phonePattern.matches(phone.trim())) ContactFieldError.InvalidPhone else null,
        emailError = if (email.isNotBlank() && (email.length > 254 || !emailPattern.matches(email.trim()))) ContactFieldError.InvalidEmail else null,
        addressError = if (address.length > 200) ContactFieldError.AddressTooLong else null,
    )
}
