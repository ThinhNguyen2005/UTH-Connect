package com.giathinh.uthconnect.data.repository

import com.giathinh.uthconnect.domain.model.Ticket
import com.giathinh.uthconnect.domain.model.TicketStatus
import com.giathinh.uthconnect.domain.repository.TicketRepository
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

@Singleton
class SupabaseTicketRepository @Inject constructor(
    private val client: SupabaseRestClient,
) : TicketRepository {
    override suspend fun getTickets(studentId: String): Result<List<Ticket>> = runCatching {
        val uid = client.userId
        val student = client.table("profiles?select=student_id&id=eq.$uid").jsonArray
            .singleOrNull()?.jsonObject?.string("student_id")
        check(student == studentId) { "Invalid student session" }
        val registrations = client.table(
            "registrations?select=event_id,status&user_id=eq.$uid&order=registered_at.desc",
        ).jsonArray.map { it.jsonObject }
        val attended = client.table("attendance?select=event_id&user_id=eq.$uid")
            .jsonArray.mapNotNull { it.jsonObject.string("event_id") }.toSet()
        registrations.mapNotNull { row ->
            val eventId = row.string("event_id") ?: return@mapNotNull null
            val event = client.table("events?select=*&id=eq.$eventId")
                .jsonArray.singleOrNull()?.jsonObject ?: return@mapNotNull null
            Ticket(
                id = "$eventId:$uid",
                studentId = studentId,
                eventId = eventId,
                eventTitle = event.string("title").orEmpty(),
                eventLocation = event.string("location").orEmpty(),
                eventStartsAt = event.string("start_at")?.let {
                    DateTimeFormatter.ofPattern("dd/MM/yyyy · HH:mm")
                        .withZone(ZoneId.of("Asia/Ho_Chi_Minh")).format(Instant.parse(it))
                }.orEmpty(),
                status = when {
                    row.string("status") == "CANCELLED" -> TicketStatus.CANCELLED
                    eventId in attended -> TicketStatus.CHECKED_IN
                    else -> TicketStatus.VALID
                },
                fallbackCode = eventId.replace("-", "").take(8).uppercase(),
                organizerName = event.string("organizer_name"),
            )
        }
    }

    override suspend fun issueCheckInToken(eventId: String): Result<String> = runCatching {
        client.rpc("issue_ticket_qr", buildJsonObject { put("p_event_id", java.util.UUID.fromString(eventId).toString()) })
            .jsonPrimitive.content
    }

    override suspend fun checkIn(token: String): Result<Unit> = runCatching {
        client.rpc("check_in_ticket_qr", buildJsonObject { put("p_token", token) })
        Unit
    }
}
