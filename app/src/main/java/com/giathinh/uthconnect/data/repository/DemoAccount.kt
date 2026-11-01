package com.giathinh.uthconnect.data.repository

import com.giathinh.uthconnect.domain.model.StudentProfile
import com.giathinh.uthconnect.domain.model.User

/** Single source for the demo login and profile; no real credentials. */
internal object DemoAccount {
    const val PASSWORD = "UTH@123456"
    val user = User(
        uid = "demo-uid-001",
        studentId = "2023040123",
        email = "demo@ut.edu.vn",
        displayName = "Nguyễn Minh Anh",
        className = "CN23A",
        major = "Công nghệ thông tin",
        faculty = "Khoa Công nghệ thông tin",
    )
    val profile = StudentProfile(
        studentId = user.studentId,
        email = user.email,
        displayName = user.displayName,
        className = user.className,
        major = user.major,
        faculty = user.faculty,
        avatarUrl = user.avatarUrl,
    )
}
