package com.giathinh.uthconnect.domain.model

/** Local demo role. In production, authorization is checked by trusted server claims. */
enum class UserRole {
    STUDENT,
    ORGANIZER,
    LECTURER,
    ADMIN,
    ;

    /** Preserve the existing organizer UI for lecturers; the server enforces access. */
    val hasOrganizerCapabilities: Boolean get() = this == ORGANIZER || this == LECTURER
}
