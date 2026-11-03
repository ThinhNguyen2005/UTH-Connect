package com.giathinh.uthconnect.domain.model

/** Student attributes. Only personal contact fields can be edited by the user. */
data class StudentProfile(
    val studentId: String,
    val email: String,
    val displayName: String,
    val className: String,
    val major: String,
    val faculty: String,
    val avatarUrl: String? = null,
    val phoneNumber: String = "",
    val personalEmail: String = "",
    val currentAddress: String = "",
    val role: UserRole = UserRole.STUDENT,
    val uthVerified: Boolean = false,
    val authUid: String = "",
) {
    val sessionIdentity: String get() = authUid.ifBlank { studentId }
    // Tài khoản mới có UID nhưng chưa có MSSV được nhà trường đối chiếu.
    init { require(studentId.isNotBlank() || authUid.isNotBlank()) { "studentId or authUid is required" } }
}
