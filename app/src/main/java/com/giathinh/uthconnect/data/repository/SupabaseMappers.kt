package com.giathinh.uthconnect.data.repository

import com.giathinh.uthconnect.domain.model.Event
import com.giathinh.uthconnect.domain.model.EventStatus
import com.giathinh.uthconnect.domain.model.StudentProfile
import com.giathinh.uthconnect.domain.model.UserRole
import java.time.Instant
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

internal fun JsonObject.string(name: String): String? =
    (get(name) as? JsonPrimitive)?.takeIf { it.isString }?.content

internal fun JsonObject.int(name: String): Int =
    (get(name) as? JsonPrimitive)?.intOrNull ?: 0

internal fun JsonObject.bool(name: String): Boolean =
    (get(name) as? JsonPrimitive)?.booleanOrNull ?: false

internal fun JsonObject.toDomainEvent(): Event = Event(
    id = requireNotNull(string("id")) { "Missing event ID" },
    title = string("title").orEmpty(),
    description = string("description").orEmpty(),
    orgId = string("organizer_id").orEmpty(),
    orgName = string("organizer_name").orEmpty(),
    location = string("location").orEmpty(),
    campus = string("campus").orEmpty(),
    startAt = Instant.parse(requireNotNull(string("start_at"))).toEpochMilli(),
    endAt = Instant.parse(requireNotNull(string("end_at"))).toEpochMilli(),
    capacity = int("max_participants"),
    registeredCount = int("current_registered"),
    status = EventStatus.fromString(string("status")),
    coverUrl = string("banner_url"),
)

internal fun JsonObject.toDomainProfile(): StudentProfile {
    val studentId = string("student_id")
    require(!studentId.isNullOrBlank() && bool("student_id_verified")) {
        "MSSV chưa được quản trị viên xác minh"
    }
    return StudentProfile(
        studentId = studentId,
        email = string("email").orEmpty(),
        displayName = string("display_name").orEmpty(),
        className = string("class_name") ?: "Chưa cập nhật",
        major = string("major") ?: "Chưa cập nhật",
        faculty = string("faculty") ?: "Chưa cập nhật",
        avatarUrl = string("avatar_url"),
        phoneNumber = string("phone_number").orEmpty(),
        role = when (string("role")) {
            "admin" -> UserRole.ADMIN
            "organizer" -> UserRole.ORGANIZER
            else -> UserRole.STUDENT
        },
        uthVerified = true,
    )
}
