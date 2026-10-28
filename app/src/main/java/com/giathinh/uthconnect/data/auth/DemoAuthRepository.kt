package com.giathinh.uthconnect.data.auth

import com.giathinh.uthconnect.domain.model.StudentProfile
import com.giathinh.uthconnect.domain.repository.AuthRepository

/**
 * Fake offline identity store ONLY for the grading/demo scenario.
 * Never reuse hard-coded credentials for production authentication.
 */
class DemoAuthRepository : AuthRepository {
    private val demoProfile = StudentProfile(
        studentId = "2023040123",
        email = "demo@ut.edu.vn",
        fullName = "Nguyễn Minh Anh",
        className = "CN23A",
        major = "Công nghệ thông tin",
        faculty = "Khoa Công nghệ thông tin",
        avatarUrl = null,
    )

    override suspend fun authenticate(identifier: String, password: String): StudentProfile? {
        val isSameStudent = identifier.equals(demoProfile.email, ignoreCase = true) ||
            identifier == demoProfile.studentId
        return if (isSameStudent && password == DEMO_PASSWORD) demoProfile else null
    }

    override suspend fun getStudentProfile(studentId: String): StudentProfile? =
        demoProfile.takeIf { it.studentId == studentId }

    companion object {
        const val DEMO_PASSWORD = "UTH@123456"
    }
}
