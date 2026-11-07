package com.giathinh.uthconnect.domain.model

/** Local demo role. In production, authorization is checked by trusted server claims. */
enum class UserRole {
    STUDENT,
    ORGANIZER,
    ADMIN,
    TEACHER;

    val isTeacherOrOrganizer: Boolean
        get() = this == TEACHER || this == ORGANIZER || this == ADMIN
}
