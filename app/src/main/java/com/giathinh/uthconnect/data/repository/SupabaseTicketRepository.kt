package com.giathinh.uthconnect.data.repository

import com.giathinh.uthconnect.domain.model.Ticket
import com.giathinh.uthconnect.domain.repository.TicketRepository
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
        val uid = uuid(client.userId)
        val student = client.table("profiles?select=student_id&id=eq.${urlEncode(uid)}").jsonArray
            .singleOrNull()?.jsonObject?.string("student_id")
        check(student == studentId) { "Invalid student session" }
        val registrations = client.table(
            "registrations?select=event_id,status,events(*)&user_id=eq.${urlEncode(uid)}&order=registered_at.desc",
        ).jsonArray.map { it.jsonObject }
        val attended = client.table("attendance?select=event_id&user_id=eq.${urlEncode(uid)}")
            .jsonArray.mapNotNull { it.jsonObject.string("event_id") }.toSet()
        registrations.mapNotNull { row ->
            row.toDomainTicket(
                studentId = studentId,
                userId = uid,
                attendedEventIds = attended,
            )
        }
    }

    override suspend fun issueCheckInToken(eventId: String): Result<String> = runCatching {
        client.rpc("issue_checkin_token", buildJsonObject { put("p_event_id", uuid(eventId)) })
            .jsonPrimitive.content
    }

    override suspend fun checkIn(token: String): Result<Unit> = runCatching {
        client.rpc("check_in", buildJsonObject { put("p_token", token) })
        return@runCatching
    }
}
