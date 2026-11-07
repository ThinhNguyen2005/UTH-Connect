package com.giathinh.uthconnect.data.repository

import com.giathinh.uthconnect.domain.model.Event
import com.giathinh.uthconnect.domain.model.EventStatus
import com.giathinh.uthconnect.domain.model.ItemCategory
import com.giathinh.uthconnect.domain.model.ListingKind
import com.giathinh.uthconnect.domain.model.LostFoundListing
import com.giathinh.uthconnect.domain.model.StudentProfile
import com.giathinh.uthconnect.domain.model.Ticket
import com.giathinh.uthconnect.domain.model.TicketStatus
import com.giathinh.uthconnect.domain.model.UserRole
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

private val TICKET_DATE_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd/MM/yyyy · HH:mm")
        .withZone(ZoneId.of("Asia/Ho_Chi_Minh"))

/**
 * Validates and normalizes a string as a standard UUID representation.
 */
internal fun uuid(id: String): String = UUID.fromString(id).toString()

/**
 * URL-encodes a string using UTF-8 encoding.
 */
internal fun urlEncode(value: String): String = URLEncoder.encode(value, StandardCharsets.UTF_8.name())

/**
 * Retrieves an optional string property from a [JsonObject].
 */
internal fun JsonObject.string(name: String): String? = (get(name) as? JsonPrimitive)?.takeIf { it.isString }?.content

/**
 * Retrieves an integer property from a [JsonObject], defaulting to 0 when absent.
 */
internal fun JsonObject.int(name: String): Int = (get(name) as? JsonPrimitive)?.intOrNull ?: 0

/**
 * Retrieves a boolean property from a [JsonObject], defaulting to false when absent.
 */
internal fun JsonObject.bool(name: String): Boolean = (get(name) as? JsonPrimitive)?.booleanOrNull ?: false

/**
 * Maps a PostgREST JSON object representing an event row to domain [Event].
 */
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

/**
 * Maps a PostgREST JSON object representing a student profile to domain [StudentProfile].
 */
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
            else -> UserRole.STUDENT
        },
        uthVerified = true,
    )
}

/**
 * Maps a PostgREST JSON object to domain [Ticket].
 */
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

/**
 * Maps a PostgREST JSON object representing a row from `lost_found_items` to a domain [LostFoundListing].
 *
 * @return Mapped [LostFoundListing] model.
 */
internal fun JsonObject.toDomainLostFoundListing(): LostFoundListing = LostFoundListing(
    id = string("id").orEmpty(),
    title = string("title").orEmpty(),
    description = string("description").orEmpty(),
    kind = runCatching { ListingKind.valueOf(string("kind").orEmpty()) }.getOrDefault(ListingKind.LOST),
    category = runCatching { ItemCategory.valueOf(string("category").orEmpty()) }.getOrDefault(ItemCategory.OTHER),
    location = string("location").orEmpty(),
    date = string("date").orEmpty(),
    contactName = string("contact_name").orEmpty(),
    contactPhone = string("contact_phone").orEmpty(),
    imageUrl = string("image_url"),
    isResolved = bool("is_resolved"),
)

/**
 * Serializes a domain [LostFoundListing] into a PostgREST JSON object payload.
 *
 * @param userId Optional user UUID to associate with the listing.
 * @return JSON object ready for insert or update queries.
 */
internal fun LostFoundListing.toJsonObject(userId: String? = null): JsonObject = buildJsonObject {
    if (id.isNotBlank() && runCatching { UUID.fromString(id) }.isSuccess) {
        put("id", id)
    }
    put("title", title)
    put("description", description)
    put("kind", kind.name)
    put("category", category.name)
    put("location", location)
    put("date", date)
    put("contact_name", contactName)
    put("contact_phone", contactPhone)
    imageUrl?.let { put("image_url", it) }
    put("is_resolved", isResolved)
    userId?.let { put("user_id", it) }
}
