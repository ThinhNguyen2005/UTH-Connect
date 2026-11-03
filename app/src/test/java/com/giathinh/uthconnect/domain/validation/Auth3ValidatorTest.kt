package com.giathinh.uthconnect.domain.validation

import org.junit.Assert.*
import org.junit.Test

class Auth3ValidatorTest {
    private val validator = LoginValidator()
    @Test fun `normalizes only exact university domain`() {
        assertEquals("student@ut.edu.vn", validator.normalizeEmail(" Student@UT.EDU.VN "))
        assertNull(validator.emailError(" Student@UT.EDU.VN "))
        listOf("x@gmail.com", "x@ut.edu.vn.evil.com", "x@sub.ut.edu.vn", "x@ut.edu.vn.",
            "@ut.edu.vn", "a@@ut.edu.vn", "a..b@ut.edu.vn", ".a@ut.edu.vn", "a.@ut.edu.vn",
            "a b@ut.edu.vn", "a\n@ut.edu.vn", "a".repeat(65) + "@ut.edu.vn").forEach {
            assertNotNull("Must reject $it", validator.emailError(it))
        }
    }
    @Test fun `new passwords require length and mixed characters`() {
        listOf("", "short", "longpassword", "Password123", "Password@", "PASSWORD@123", "Password 123@",
            "Aa1@".repeat(17)).forEach { assertNotNull(validator.signUpPasswordError(it)) }
        assertNull(validator.signUpPasswordError("Correct@123"))
    }
    @Test fun `existing passwords remain usable for sign in`() {
        assertNull(validator.passwordError("legacy-password"))
        assertNotNull(validator.passwordError(""))
    }
}
