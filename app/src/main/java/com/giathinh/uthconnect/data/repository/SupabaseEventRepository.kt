package com.giathinh.uthconnect.data.repository

import com.giathinh.uthconnect.domain.model.Event
import com.giathinh.uthconnect.domain.model.EventDraft
import com.giathinh.uthconnect.domain.model.EventStatus
import java.time.Instant
import com.giathinh.uthconnect.domain.repository.EventRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put

@Singleton
class SupabaseEventRepository @Inject constructor(
    private val client: SupabaseRestClient,
) : EventRepository {
    private val revision = MutableStateFlow(0L)

    override fun getEvents(campus: String?): Flow<List<Event>> = revision.map {
        val rows = client.table("events?select=*&order=start_at.asc").jsonArray
        rows.map { row -> row.jsonObject.toDomainEvent() }
            .filter { campus.isNullOrBlank() || campus == "Tất cả" || it.campus == campus }
    }

    override suspend fun getEventDetail(eventId: String): Result<Event?> = runCatching {
        client.table("events?select=*&id=eq.${uuid(eventId)}").jsonArray
            .singleOrNull()?.jsonObject?.toDomainEvent()
    }

    override suspend fun registerEvent(
        eventId: String, uid: String, studentId: String, studentName: String,
    ): Result<Unit> = runCatching {
        check(uid == client.userId) { "Cannot register for another user" }
        client.rpc("register_for_event", buildJsonObject { put("p_event_id", uuid(eventId)) })
        refresh()
    }

    override suspend fun cancelRegistration(eventId: String, uid: String): Result<Unit> = runCatching {
        check(uid == client.userId) { "Cannot cancel another user's registration" }
        client.rpc("cancel_event_registration", buildJsonObject { put("p_event_id", uuid(eventId)) })
        refresh()
    }

    override fun isRegistered(eventId: String, uid: String): Flow<Boolean> = revision.map {
        if (uid != client.userId) return@map false
        client.table("registrations?select=status&event_id=eq.${uuid(eventId)}&user_id=eq.$uid")
            .jsonArray.any { row -> row.jsonObject.string("status") == "REGISTERED" }
    }

    override suspend fun registerCurrentUser(eventId: String): Result<Unit> = registerEvent(
        eventId = eventId, uid = client.userId, studentId = "", studentName = "",
    )

    override suspend fun cancelCurrentRegistration(eventId: String): Result<Unit> =
        cancelRegistration(eventId, client.userId)

    override fun currentUserRegistrations(): Flow<Set<String>> = revision.map {
        client.table("registrations?select=event_id&status=eq.REGISTERED&user_id=eq.${client.userId}")
            .jsonArray.mapNotNull { row -> row.jsonObject.string("event_id") }.toSet()
    }

    override fun refresh() { revision.value++ }

    override fun currentUserId(): String = client.userId

    override suspend fun saveEvent(eventId: String?, draft: EventDraft): Result<Unit> = runCatching {
        val inputs = buildJsonObject {
            if (eventId != null) put("p_event_id", uuid(eventId))
            put("p_title", draft.title)
            put("p_description", draft.description)
            put("p_location", draft.location)
            put("p_campus", draft.campus)
            put("p_start_at", Instant.ofEpochMilli(draft.startAt).toString())
            put("p_end_at", Instant.ofEpochMilli(draft.endAt).toString())
            put("p_max_participants", draft.capacity)
        }
        client.rpc(if (eventId == null) "create_event" else "edit_event", inputs)
        refresh()
    }

    override suspend fun setStatus(eventId: String, status: EventStatus): Result<Unit> = runCatching {
        client.rpc("set_event_status", buildJsonObject {
            put("p_event_id", uuid(eventId))
            put("p_status", status.name)
        })
        refresh()
    }

    private fun uuid(id: String): String = java.util.UUID.fromString(id).toString()
}
