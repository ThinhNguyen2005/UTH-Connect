package com.giathinh.uthconnect.domain.validation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LoginValidatorTest {
    private val validator = LoginValidator()

    @Test fun `MSSV 8-12 chu so hop le`() {
        assertNull(validator.identifierError("2023040123"))
        assertNull(validator.identifierError(" 2023040123 "))
    }

    @Test fun `Email ut edu vn hop le khong phan biet hoa thuong`() {
        assertNull(validator.identifierError("demo@ut.edu.vn"))
        assertNull(validator.identifierError("DEMO@UT.EDU.VN"))
    }

    @Test fun `teacher IDs must match shared validation format`() {
        assertNull(validator.identifierError("GV2023001"))
        assertEquals(LoginFieldError.InvalidIdentifier, validator.identifierError("GVabcdefgh"))
        assertTrue(validator.isTeacherId("GV2023001"))
        assertFalse(validator.isTeacherId("GVabcdefgh"))
    }

    @Test fun `Khong chap nhan email ngoai truong hoac MSSV sai`() {
        assertEquals(LoginFieldError.InvalidIdentifier, validator.identifierError("test@gmail.com"))
        assertEquals(LoginFieldError.InvalidIdentifier, validator.identifierError("2023A40123"))
        assertEquals(LoginFieldError.InvalidIdentifier, validator.identifierError("123"))
    }

    @Test fun `Khong de trong MSSV va mat khau`() {
        assertEquals(LoginFieldError.Required, validator.identifierError("   "))
        assertEquals(LoginFieldError.Required, validator.passwordError("   "))
    }
}
