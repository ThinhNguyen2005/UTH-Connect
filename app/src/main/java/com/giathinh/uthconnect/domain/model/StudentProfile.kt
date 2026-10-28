package com.giathinh.uthconnect.domain.model

/** Domain entity: never stores a password or an authentication token. */
data class StudentProfile(
    val studentId: String,
    val email: String,
    val fullName: String,
    val className: String,
    val major: String,
    val faculty: String,
    val avatarUrl: String? = null,
)
