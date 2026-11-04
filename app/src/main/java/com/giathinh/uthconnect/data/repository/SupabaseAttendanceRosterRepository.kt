package com.giathinh.uthconnect.data.repository

import com.giathinh.uthconnect.domain.model.AttendanceEntry
import com.giathinh.uthconnect.domain.model.AttendanceRoster
import com.giathinh.uthconnect.domain.repository.AttendanceRosterRepository
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject

class SupabaseAttendanceRosterRepository @Inject constructor(
    private val client: SupabaseRestClient,
) : AttendanceRosterRepository {
    override suspend fun getRoster(eventId: String): Result<AttendanceRoster> = try {
        Result.success(loadRoster(UUID.fromString(eventId).toString()))
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        Result.failure(error)
    }

    private suspend fun loadRoster(eventId: String): AttendanceRoster {
        val event = client.table("events?select=id,title,organizer_id&id=eq.$eventId")
            .jsonArray.singleOrNull()?.jsonObject ?: error("Event unavailable")
        val uid = client.userId
        val role = client.table("profiles?select=role&id=eq.$uid")
            .jsonArray.singleOrNull()?.jsonObject?.string("role")
        check(role == "admin" || role == "organizer" && event.string("organizer_id") == uid) {
            "FORBIDDEN"
        }
        val registrations = allRows(
            "registrations?select=user_id,student_id,display_name,email,status&event_id=eq.$eventId" +
                "&status=eq.REGISTERED&order=user_id.asc",
        )
        val attendance = allRows(
            "attendance?select=user_id,checked_in_at,checked_in_by&event_id=eq.$eventId&order=user_id.asc",
        ).associateBy { it.string("user_id") }
        return AttendanceRoster(
            eventId = eventId,
            eventTitle = event.string("title").orEmpty(),
            entries = registrations.map { registration ->
                val userId = requireNotNull(registration.string("user_id"))
                val checkIn = attendance[userId]
                AttendanceEntry(
                    userId = userId,
                    studentId = registration.string("student_id").orEmpty(),
                    displayName = registration.string("display_name").orEmpty(),
                    email = registration.string("email").orEmpty(),
                    checkedInAt = checkIn?.string("checked_in_at"),
                    checkedInBy = checkIn?.string("checked_in_by"),
                )
            },
        )
    }

    private suspend fun allRows(query: String): List<JsonObject> {
        val result = mutableListOf<JsonObject>()
        var offset = 0
        do {
            val page = client.table("$query&limit=$PAGE_SIZE&offset=$offset")
                .jsonArray.map { it.jsonObject }
            result += page
            offset += page.size
        } while (page.size == PAGE_SIZE)
        return result
    }

    private companion object { const val PAGE_SIZE = 500 }
}
