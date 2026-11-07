package com.giathinh.uthconnect.data.repository

import com.giathinh.uthconnect.domain.model.Event
import com.giathinh.uthconnect.domain.model.EventStatus
import com.giathinh.uthconnect.domain.model.StudentProfile
import com.giathinh.uthconnect.domain.model.Ticket
import com.giathinh.uthconnect.domain.model.TicketStatus
import com.giathinh.uthconnect.domain.model.UserRole
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

private val TICKET_DATE_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd/MM/yyyy · HH:mm")
        .withZone(ZoneId.of("Asia/Ho_Chi_Minh"))

internal fun uuid(id: String): String = UUID.fromString(id).toString()

internal fun urlEncode(value: String): String = URLEncoder.encode(value, StandardCharsets.UTF_8.name())

internal fun JsonObject.string(name: String): String? = (get(name) as? JsonPrimitive)?.takeIf { it.isString }?.content

internal fun JsonObject.int(name: String): Int = (get(name) as? JsonPrimitive)?.intOrNull ?: 0

internal fun JsonObject.bool(name: String): Boolean = (get(name) as? JsonPrimitive)?.booleanOrNull ?: false

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
    points = int("points"),
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
        personalEmail = string("personal_email").orEmpty(),
        currentAddress = string("current_address").orEmpty(),
        role = when (string("role")) {
            "admin" -> UserRole.ADMIN
            "organizer" -> UserRole.ORGANIZER
            "teacher" -> UserRole.TEACHER
            else -> UserRole.STUDENT
        },
        uthVerified = true,
    )
}

internal fun JsonObject.toDomainTicket(
    studentId: String,
    userId: String,
    attendedEventIds: Set<String>,
): Ticket? {
    val eventId = string("event_id") ?: return null
    val event = (get("events") as? JsonObject)
        ?: (get("event") as? JsonObject)
        ?: (get("events") as? JsonArray)?.firstOrNull() as? JsonObject
        ?: return null
    val statusStr = string("status")
    return Ticket(
        id = "$eventId:$userId",
        studentId = studentId,
        eventId = eventId,
        eventTitle = event.string("title").orEmpty(),
        eventLocation = event.string("location").orEmpty(),
        eventStartsAt = event.string("start_at")?.let {
            runCatching { TICKET_DATE_FORMATTER.format(Instant.parse(it)) }.getOrNull()
        }.orEmpty(),
        status = when {
            statusStr == "CANCELLED" -> TicketStatus.CANCELLED
            eventId in attendedEventIds -> TicketStatus.CHECKED_IN
            else -> TicketStatus.VALID
        },
        fallbackCode = eventId.replace("-", "").take(8).uppercase(),
        organizerName = event.string("organizer_name"),
    )
}
