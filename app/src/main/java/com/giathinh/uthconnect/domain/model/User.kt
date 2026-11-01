package com.giathinh.uthconnect.domain.model

/**
 * Data Model thông tin người dùng / sinh viên UTH chuẩn dùng chung toàn dự án.
 */
data class User(
    val uid: String,
    val studentId: String,
    val email: String,
    val displayName: String,
    val className: String,
    val major: String,
    val faculty: String,
    val avatarUrl: String? = null,
    val phoneNumber: String? = null,
    val role: String = "student", // "student" | "organizer" | "admin"
)
