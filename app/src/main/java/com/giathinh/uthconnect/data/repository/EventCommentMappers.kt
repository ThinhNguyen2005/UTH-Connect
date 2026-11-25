package com.giathinh.uthconnect.data.repository

import com.giathinh.uthconnect.domain.model.EventComment
import com.giathinh.uthconnect.domain.model.StudentProfile
import com.giathinh.uthconnect.domain.model.UserRole
import kotlinx.serialization.json.JsonObject
import java.time.Instant

/**
 * Maps a PostgREST JSON object representing an event comment row to domain [EventComment].
 */
internal fun JsonObject.toDomainEventComment(): EventComment = EventComment(
    id = requireNotNull(string("id")) { "Missing comment ID" },
    eventId = string("event_id").orEmpty(),
    authorId = string("user_id").orEmpty(),
    authorName = string("author_name").orEmpty(),
    authorStudentId = string("student_id").orEmpty(),
    authorClass = string("author_class").orEmpty(),
    authorMajor = string("author_major").orEmpty(),
    authorAvatarUrl = string("author_avatar_url"),
    content = string("content").orEmpty(),
    createdAt = runCatching { Instant.parse(string("created_at")).toEpochMilli() }
        .getOrDefault(System.currentTimeMillis()),
)

/**
 * Maps a PostgREST JSON object to a guest-safe [StudentProfile], hiding private contact info.
 */
internal fun JsonObject.toGuestDomainProfile(): StudentProfile {
    val studentId = string("student_id") ?: "SV"
    return StudentProfile(
        studentId = studentId,
        email = string("email").orEmpty(),
        displayName = string("display_name").orEmpty().ifBlank { "Sinh viên UTH" },
        className = string("class_name") ?: "Chưa cập nhật",
        major = string("major") ?: "Chưa cập nhật",
        faculty = string("faculty") ?: "Chưa cập nhật",
        avatarUrl = string("avatar_url"),
        phoneNumber = "",
        personalEmail = "",
        currentAddress = "",
        role = when (string("role")) {
            "admin" -> UserRole.ADMIN
            "organizer", "lecturer" -> UserRole.ORGANIZER
            else -> UserRole.STUDENT
        },
        uthVerified = bool("student_id_verified"),
    )
}
