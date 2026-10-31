package com.giathinh.uthconnect.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StudentProfileRoleTest {
    private fun profile() = StudentProfile(
        "2023040123", "demo@ut.edu.vn", "Nguyễn Minh Anh",
        "CN23A", "Công nghệ thông tin", "Khoa Công nghệ thông tin",
    )

    @Test fun `old constructors default to student and unverified`() {
        val student = profile()
        assertEquals(UserRole.STUDENT, student.role)
        assertFalse(student.uthVerified)
        assertEquals("2023040123", student.studentId)
    }

    @Test fun `role and verification survive editing contact with copy`() {
        val organizer = profile().copy(role = UserRole.ORGANIZER, uthVerified = true)
        val edited = organizer.copy(phoneNumber = "0912345678")
        assertEquals(UserRole.ORGANIZER, edited.role)
        assertTrue(edited.uthVerified)
        assertEquals("0912345678", edited.phoneNumber)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `studentId must be present`() { profile().copy(studentId = "") }
}
