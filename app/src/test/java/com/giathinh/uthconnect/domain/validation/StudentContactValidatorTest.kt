package com.giathinh.uthconnect.domain.validation

import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test

class StudentContactValidatorTest {
    private val validator = StudentContactValidator()
    @Test fun `accept optional empty contact`() = assertTrue(validator.validate("", "", "").valid)
    @Test fun `accept vietnam phone and personal email`() = assertTrue(
        validator.validate("0912345678", "user@gmail.com", "123 Main Road").valid)
    @Test fun `reject invalid phone`() = assertEquals(ContactFieldError.InvalidPhone,
        validator.validate("123", "", "").phoneError)
    @Test fun `reject email without domain`() = assertEquals(ContactFieldError.InvalidEmail,
        validator.validate("", "abc@", "").emailError)
    @Test fun `reject overlong address`() = assertEquals(ContactFieldError.AddressTooLong,
        validator.validate("", "", "a".repeat(201)).addressError)
}
