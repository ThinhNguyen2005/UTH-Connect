package com.giathinh.uthconnect.data.repository

import android.content.Context
import com.giathinh.uthconnect.domain.model.Event
import com.giathinh.uthconnect.domain.model.EventDraft
import com.giathinh.uthconnect.domain.model.EventStatus
import com.giathinh.uthconnect.domain.repository.EventRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupabaseEventRepository @Inject constructor(
    private val client: SupabaseRestClient,
    @ApplicationContext private val context: Context,
) : EventRepository {
    private val revision = MutableStateFlow(0L)
    private val prefs = context.getSharedPreferences("uth_events_database", Context.MODE_PRIVATE)

    private fun loadPersistedEvents(): List<Event> {
        val raw = prefs.getString("events_list", null) ?: return emptyList()
        return runCatching {
            val array = Json.parseToJsonElement(raw).jsonArray
            array.mapNotNull { element ->
                val obj = element.jsonObject
                val id = obj["id"]?.jsonPrimitive?.content ?: return@mapNotNull null
                val title = obj["title"]?.jsonPrimitive?.content ?: ""
                val description = obj["description"]?.jsonPrimitive?.content ?: ""
                val orgId = obj["orgId"]?.jsonPrimitive?.content ?: ""
                val orgName = obj["orgName"]?.jsonPrimitive?.content ?: ""
                val location = obj["location"]?.jsonPrimitive?.content ?: ""
                val campus = obj["campus"]?.jsonPrimitive?.content ?: "Cơ sở 1"
                val startAt = obj["startAt"]?.jsonPrimitive?.content?.toLongOrNull() ?: 0L
                val endAt = obj["endAt"]?.jsonPrimitive?.content?.toLongOrNull() ?: 0L
                val capacity = obj["capacity"]?.jsonPrimitive?.content?.toIntOrNull() ?: 100
                val registeredCount = obj["registeredCount"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0
                val statusStr = obj["status"]?.jsonPrimitive?.content
                val status = EventStatus.fromString(statusStr)
                val coverUrl = obj["coverUrl"]?.jsonPrimitive?.content
                Event(
                    id = id,
                    title = title,
                    description = description,
                    orgId = orgId,
                    orgName = orgName,
                    location = location,
                    campus = campus,
                    startAt = startAt,
                    endAt = endAt,
                    capacity = capacity,
                    registeredCount = registeredCount,
                    status = status,
                    coverUrl = coverUrl,
                )
            }
        }.getOrDefault(emptyList())
    }

    private fun savePersistedEvents(events: List<Event>) {
        val jsonArray = buildJsonArray {
            for (e in events) {
                add(
                    buildJsonObject {
                        put("id", e.id)
                        put("title", e.title)
                        put("description", e.description)
                        put("orgId", e.orgId)
                        put("orgName", e.orgName)
                        put("location", e.location)
                        put("campus", e.campus)
                        put("startAt", e.startAt.toString())
                        put("endAt", e.endAt.toString())
                        put("capacity", e.capacity)
                        put("registeredCount", e.registeredCount)
                        put("status", e.status.name)
                        e.coverUrl?.let { put("coverUrl", it) }
                    },
                )
            }
        }
        prefs.edit().putString("events_list", jsonArray.toString()).apply()
    }

    private fun loadPersistedRegistrations(): Set<String> {
        return prefs.getStringSet("registered_event_ids", emptySet()) ?: emptySet()
    }

    private fun savePersistedRegistrations(ids: Set<String>) {
        prefs.edit().putStringSet("registered_event_ids", ids).apply()
    }

    override fun getEvents(campus: String?): Flow<List<Event>> = revision.map {
        val remoteEvents = runCatching {
            val rows = client.table("events?select=*&order=start_at.asc").jsonArray
            rows.map { row -> row.jsonObject.toDomainEvent() }
        }.getOrDefault(emptyList())

        val localEvents = loadPersistedEvents()
        val allEventsMap = LinkedHashMap<String, Event>()
        for (event in localEvents) {
            allEventsMap[event.id] = event
        }
        for (event in remoteEvents) {
            allEventsMap[event.id] = event
        }
        allEventsMap.values.toList()
            .filter { campus.isNullOrBlank() || campus == "Tất cả" || it.campus == campus }
    }

    override suspend fun getEventDetail(eventId: String): Result<Event?> = runCatching {
        loadPersistedEvents().firstOrNull { it.id == eventId }
            ?: runCatching {
                client.table("events?select=*&id=eq.${urlEncode(uuid(eventId))}").jsonArray
                    .singleOrNull()?.jsonObject?.toDomainEvent()
            }.getOrNull()
    }

    override suspend fun registerEvent(
        eventId: String,
        uid: String,
        studentId: String,
        studentName: String,
    ): Result<Unit> = runCatching {
        if (client.userId.isNotBlank() && uid == client.userId) {
            runCatching {
                client.rpc("register_for_event", buildJsonObject { put("p_event_id", uuid(eventId)) })
            }
        }
        val current = loadPersistedRegistrations().toMutableSet()
        current.add(eventId)
        savePersistedRegistrations(current)
        refresh()
    }

    override suspend fun cancelRegistration(eventId: String, uid: String): Result<Unit> = runCatching {
        if (client.userId.isNotBlank() && uid == client.userId) {
            runCatching {
                client.rpc("cancel_event_registration", buildJsonObject { put("p_event_id", uuid(eventId)) })
            }
        }
        val current = loadPersistedRegistrations().toMutableSet()
        current.remove(eventId)
        savePersistedRegistrations(current)
        refresh()
    }

    override fun isRegistered(eventId: String, uid: String): Flow<Boolean> = revision.map {
        val localRegistered = loadPersistedRegistrations().contains(eventId)
        if (localRegistered) return@map true
        if (uid.isBlank() || uid != client.userId) return@map false
        runCatching {
            client.table("registrations?select=status&event_id=eq.${urlEncode(uuid(eventId))}&user_id=eq.${urlEncode(uuid(uid))}")
                .jsonArray.any { row -> row.jsonObject.string("status") == "REGISTERED" }
        }.getOrDefault(false)
    }

    override suspend fun registerCurrentUser(eventId: String): Result<Unit> = registerEvent(
        eventId = eventId,
        uid = client.userId,
        studentId = "",
        studentName = "",
    )

    override suspend fun cancelCurrentRegistration(eventId: String): Result<Unit> = cancelRegistration(eventId, client.userId)

    override fun currentUserRegistrations(): Flow<Set<String>> = revision.map {
        val remoteRegistrations = if (client.userId.isNotBlank()) {
            runCatching {
                client.table("registrations?select=event_id&status=eq.REGISTERED&user_id=eq.${urlEncode(uuid(client.userId))}")
                    .jsonArray.mapNotNull { row -> row.jsonObject.string("event_id") }.toSet()
            }.getOrDefault(emptySet())
        } else {
            emptySet()
        }
        loadPersistedRegistrations() + remoteRegistrations
    }

    override fun refresh() {
        revision.value++
    }

    override fun currentUserId(): String = client.userId

    override suspend fun saveEvent(eventId: String?, draft: EventDraft): Result<Unit> = runCatching {
        val id = eventId ?: "event-${UUID.randomUUID()}"
        val inputs = buildJsonObject {
            put("p_event_id", uuid(id))
            put("p_title", draft.title)
            put("p_description", draft.description)
            put("p_location", draft.location)
            put("p_campus", draft.campus)
            put("p_start_at", Instant.ofEpochMilli(draft.startAt).toString())
            put("p_end_at", Instant.ofEpochMilli(draft.endAt).toString())
            put("p_max_participants", draft.capacity)
        }
        if (client.userId.isNotBlank()) {
            runCatching {
                client.rpc(if (eventId == null) "create_event" else "edit_event", inputs)
            }
        }
        val currentEvents = loadPersistedEvents().toMutableList()
        val index = currentEvents.indexOfFirst { it.id == id }
        val newEvent = Event(
            id = id,
            title = draft.title,
            description = draft.description,
            orgId = client.userId.ifBlank { "GV2023001" },
            orgName = "ThS. Trần Thu Trang",
            location = draft.location,
            campus = draft.campus,
            startAt = draft.startAt,
            endAt = draft.endAt,
            capacity = draft.capacity,
            registeredCount = 0,
            status = EventStatus.PUBLISHED,
        )
        if (index >= 0) {
            currentEvents[index] = newEvent
        } else {
            currentEvents.add(newEvent)
        }
        savePersistedEvents(currentEvents)
        refresh()
    }

    override suspend fun setStatus(eventId: String, status: EventStatus): Result<Unit> = runCatching {
        if (client.userId.isNotBlank()) {
            runCatching {
                client.rpc(
                    "set_event_status",
                    buildJsonObject {
                        put("p_event_id", uuid(eventId))
                        put("p_status", status.name)
                    },
                )
            }
        }
        val currentEvents = loadPersistedEvents().toMutableList()
        val index = currentEvents.indexOfFirst { it.id == eventId }
        if (index >= 0) {
            currentEvents[index] = currentEvents[index].copy(status = status)
            savePersistedEvents(currentEvents)
        }
        refresh()
    }
}
