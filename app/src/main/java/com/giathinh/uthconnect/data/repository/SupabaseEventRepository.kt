package com.giathinh.uthconnect.data.repository

import android.content.Context
import com.giathinh.uthconnect.BuildConfig
import com.giathinh.uthconnect.domain.model.Event
import com.giathinh.uthconnect.domain.model.EventDraft
import com.giathinh.uthconnect.domain.model.EventStatus
import com.giathinh.uthconnect.domain.repository.EventRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transform
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
                val points = obj["points"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0
                val statusStr = obj["status"]?.jsonPrimitive?.content
                val status = EventStatus.fromString(statusStr)
                val coverUrl = obj["coverUrl"]?.jsonPrimitive?.content
                val category = obj["category"]?.jsonPrimitive?.content.orEmpty()
                val managers = (obj["managers"] as? kotlinx.serialization.json.JsonArray)
                    ?.mapNotNull { (it as? kotlinx.serialization.json.JsonPrimitive)?.takeIf { value -> value.isString }?.content }
                    .orEmpty()
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
                    points = points,
                    status = status,
                    coverUrl = coverUrl,
                    category = category,
                    managers = managers,
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
                        put("points", e.points)
                        put("status", e.status.name)
                        e.coverUrl?.let { put("coverUrl", it) }
                        put("category", e.category)
                        put("managers", buildJsonArray { e.managers.forEach { manager -> add(manager) } })
                    },
                )
            }
        }
        prefs.edit().putString("events_list", jsonArray.toString()).apply()
    }

    /** Keeps cached registrations isolated by their owning Supabase user ID. */
    private fun registrationStorageKey(userId: String): String = "registered_event_ids_$userId"

    private fun loadPersistedRegistrations(userId: String): Set<String> {
        if (userId.isBlank()) return emptySet()
        return prefs.getStringSet(registrationStorageKey(userId), emptySet()) ?: emptySet()
    }

    private fun savePersistedRegistrations(userId: String, ids: Set<String>) {
        require(userId.isNotBlank()) { "Registration user ID is required" }
        prefs.edit().putStringSet(registrationStorageKey(userId), ids).apply()
    }

    /** Returns an empty ID when the client has no authenticated session. */
    private fun currentUserIdOrBlank(): String = runCatching { client.userId }.getOrDefault("")

    /** Rejects signed-out writes in Supabase builds while allowing explicit local demo mode. */
    private fun requireUserOrOffline(userId: String) {
        check(userId.isNotBlank() || !BuildConfig.USE_SUPABASE) { "Supabase sign-in required" }
    }

    override fun getEvents(campus: String?): Flow<List<Event>> = revision.transform {
        val localEvents = loadPersistedEvents()
        fun List<Event>.forCampus() = filter {
            campus.isNullOrBlank() || campus == "Tất cả" || it.campus == campus
        }
        emit(localEvents.forCampus())
        if (!BuildConfig.USE_SUPABASE || currentUserIdOrBlank().isBlank()) return@transform

        val rows = client.table("events?select=*&order=start_at.asc").jsonArray
        val remoteEvents = rows.map { row -> row.jsonObject.toDomainEvent() }

        val allEventsMap = LinkedHashMap<String, Event>()
        for (event in localEvents) {
            allEventsMap[event.id] = event
        }
        for (event in remoteEvents) {
            allEventsMap[event.id] = event
        }
        emit(allEventsMap.values.toList().forCampus())
    }

    override suspend fun getEventDetail(eventId: String): Result<Event?> = runCatching {
        val local = loadPersistedEvents().firstOrNull { it.id == eventId }
        if (!BuildConfig.USE_SUPABASE || currentUserIdOrBlank().isBlank()) {
            local
        } else {
            client.table("events?select=*&id=eq.${urlEncode(uuid(eventId))}").jsonArray
                .singleOrNull()?.jsonObject?.toDomainEvent() ?: local
        }
    }

    override suspend fun registerEvent(
        eventId: String,
        uid: String,
        studentId: String,
        studentName: String,
    ): Result<Unit> = runCatching {
        val signedInUserId = currentUserIdOrBlank()
        requireUserOrOffline(signedInUserId)
        val ownerId = if (signedInUserId.isNotBlank()) {
            require(uid == signedInUserId) { "Registration user does not match signed-in user" }
            signedInUserId
        } else {
            require(uid.isNotBlank()) { "Registration user ID is required" }
            uid
        }
        if (signedInUserId.isNotBlank()) {
            client.rpc("register_for_event", buildJsonObject { put("p_event_id", uuid(eventId)) })
        }
        val current = loadPersistedRegistrations(ownerId).toMutableSet()
        current.add(eventId)
        savePersistedRegistrations(ownerId, current)
        refresh()
    }

    override suspend fun cancelRegistration(eventId: String, uid: String): Result<Unit> = runCatching {
        val signedInUserId = currentUserIdOrBlank()
        requireUserOrOffline(signedInUserId)
        val ownerId = if (signedInUserId.isNotBlank()) {
            require(uid == signedInUserId) { "Registration user does not match signed-in user" }
            signedInUserId
        } else {
            require(uid.isNotBlank()) { "Registration user ID is required" }
            uid
        }
        if (signedInUserId.isNotBlank()) {
            client.rpc("cancel_event_registration", buildJsonObject { put("p_event_id", uuid(eventId)) })
        }
        val current = loadPersistedRegistrations(ownerId).toMutableSet()
        current.remove(eventId)
        savePersistedRegistrations(ownerId, current)
        refresh()
    }

    override fun isRegistered(eventId: String, uid: String): Flow<Boolean> = revision.map {
        val signedInUserId = currentUserIdOrBlank()
        val ownerId = when {
            signedInUserId.isNotBlank() && signedInUserId == uid -> signedInUserId
            signedInUserId.isNotBlank() -> return@map false
            !BuildConfig.USE_SUPABASE && uid.isNotBlank() -> uid
            else -> return@map false
        }
        val localRegistered = loadPersistedRegistrations(ownerId).contains(eventId)
        if (localRegistered) return@map true
        if (signedInUserId.isBlank()) return@map false
        runCatching {
            client.table("registrations?select=status&event_id=eq.${urlEncode(uuid(eventId))}&user_id=eq.${urlEncode(uuid(ownerId))}")
                .jsonArray.any { row -> row.jsonObject.string("status") == "REGISTERED" }
        }.getOrDefault(false)
    }

    override suspend fun registerCurrentUser(eventId: String): Result<Unit> = registerEvent(
        eventId = eventId,
        uid = currentUserIdOrBlank(),
        studentId = "",
        studentName = "",
    )

    override suspend fun cancelCurrentRegistration(eventId: String): Result<Unit> =
        cancelRegistration(eventId, currentUserIdOrBlank())

    override fun currentUserRegistrations(): Flow<Set<String>> = revision.map {
        val userId = currentUserIdOrBlank()
        val remoteRegistrations = if (userId.isNotBlank()) {
            runCatching {
                client.table("registrations?select=event_id&status=eq.REGISTERED&user_id=eq.${urlEncode(uuid(userId))}")
                    .jsonArray.mapNotNull { row -> row.jsonObject.string("event_id") }.toSet()
            }.getOrDefault(emptySet())
        } else {
            emptySet()
        }
        loadPersistedRegistrations(userId) + remoteRegistrations
    }

    override fun refresh() {
        revision.value++
    }

    override fun currentUserId(): String = currentUserIdOrBlank()

    override suspend fun saveEvent(eventId: String?, draft: EventDraft): Result<String> = runCatching {
        // RPC parameters are UUID columns in Supabase; a prefixed local ID made creation fail remotely.
        val userId = currentUserIdOrBlank()
        requireUserOrOffline(userId)
        val inputs = buildJsonObject {
            eventId?.let { put("p_event_id", uuid(it)) }
            put("p_title", draft.title)
            put("p_description", draft.description)
            put("p_location", draft.location)
            put("p_campus", draft.campus)
            put("p_start_at", Instant.ofEpochMilli(draft.startAt).toString())
            put("p_end_at", Instant.ofEpochMilli(draft.endAt).toString())
            put("p_max_participants", draft.capacity)
            put("p_points", draft.points)
            put("p_organizer_name", draft.organizerName)
            put("p_category", draft.category)
            put("p_managers", buildJsonArray { draft.managers.forEach { manager -> add(manager) } })
        }
        val id = if (userId.isNotBlank()) {
            if (eventId == null) {
                client.rpc("create_event_v2", inputs).jsonPrimitive.content
            } else {
                client.rpc("edit_event_v2", inputs)
                eventId
            }
        } else {
            val localId = eventId ?: UUID.randomUUID().toString()
            val currentEvents = loadPersistedEvents().toMutableList()
            val index = currentEvents.indexOfFirst { it.id == localId }
            val newEvent = Event(
                id = localId,
                title = draft.title,
                description = draft.description,
                orgId = "DEMO_ORGANIZER",
                orgName = draft.organizerName,
                location = draft.location,
                campus = draft.campus,
                startAt = draft.startAt,
                endAt = draft.endAt,
                capacity = draft.capacity,
                registeredCount = 0,
                points = draft.points,
                status = EventStatus.PUBLISHED,
                category = draft.category,
                managers = draft.managers,
            )
            if (index >= 0) currentEvents[index] = newEvent else currentEvents.add(newEvent)
            savePersistedEvents(currentEvents)
            localId
        }
        refresh()
        id
    }

    override suspend fun setStatus(eventId: String, status: EventStatus): Result<Unit> = runCatching {
        val userId = currentUserIdOrBlank()
        requireUserOrOffline(userId)
        if (userId.isNotBlank()) {
            client.rpc(
                "set_event_status",
                buildJsonObject {
                    put("p_event_id", uuid(eventId))
                    put("p_status", status.name)
                },
            )
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
