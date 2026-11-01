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
) {
    init { require(studentId.isNotBlank()) { "studentId is required" } }
}
