package com.giathinh.uthconnect.domain.model

/** Thông tin sinh viên cho Profile; không chứa token/mật khẩu. */
data class StudentProfile(
    val studentId: String,
    val email: String,
    val displayName: String,
    val className: String,
    val major: String,
    val faculty: String,
    val avatarUrl: String? = null,
)
