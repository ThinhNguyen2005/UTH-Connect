package com.giathinh.uthconnect.domain.validation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test

class PasswordChangeValidatorTest {
    private val validator = PasswordChangeValidator()
    @Test fun `accept strong distinct password`() = assertTrue(validator.validate("UTH@123456", "New@12345", "New@12345").valid)
    @Test fun `reject weak password`() = assertEquals(PasswordError.Weak,
        validator.validate("UTH@123456", "abc", "abc").newError)
    @Test fun `reject confirmation mismatch`() = assertEquals(PasswordError.ConfirmationMismatch,
        validator.validate("UTH@123456", "New@12345", "New@12346").confirmationError)
    @Test fun `reject reuse of current password`() = assertEquals(PasswordError.SameAsOld,
        validator.validate("UTH@123456", "UTH@123456", "UTH@123456").newError)
    @Test fun `reject whitespace and excessive password`() = assertFalse(
        validator.validate("old", "A@abcdef1 ", "A@abcdef1 ").valid)
}
