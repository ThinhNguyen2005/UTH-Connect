package com.giathinh.uthconnect.domain.validation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LoginValidatorTest {
    private val validator = LoginValidator()

    @Test fun `empty identifier is required`() {
        assertEquals(InputError.REQUIRED, validator.validateIdentifier("   "))
    }

    @Test fun `valid student ID is accepted`() {
        assertNull(validator.validateIdentifier("2023040123"))
    }

    @Test fun `non numeric student ID is rejected`() {
        assertEquals(InputError.INVALID_IDENTIFIER, validator.validateIdentifier("2023A40123"))
    }

    @Test fun `short ID is rejected`() {
        assertEquals(InputError.INVALID_IDENTIFIER, validator.validateIdentifier("123"))
    }

    @Test fun `school email is accepted case insensitively`() {
        assertNull(validator.validateIdentifier("  Demo@UT.EDU.VN  "))
    }

    @Test fun `personal gmail is rejected`() {
        assertEquals(InputError.INVALID_IDENTIFIER, validator.validateIdentifier("demo@gmail.com"))
    }

    @Test fun `fake subdomain is rejected`() {
        assertEquals(InputError.INVALID_IDENTIFIER, validator.validateIdentifier("demo@ut.edu.vn.fake"))
    }

    @Test fun `empty password is required`() {
        assertEquals(InputError.REQUIRED, validator.validatePassword(" "))
    }

    @Test fun `nonempty password is accepted`() {
        assertNull(validator.validatePassword("UTH@123456"))
    }
}
