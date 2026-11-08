package com.giathinh.uthconnect.data.repository

import android.content.Context
import com.giathinh.uthconnect.domain.model.Course
import com.giathinh.uthconnect.domain.model.Deadline
import com.giathinh.uthconnect.domain.model.Timetable
import com.giathinh.uthconnect.domain.repository.SessionRepository
import com.giathinh.uthconnect.domain.repository.TimetableRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.time.LocalDateTime
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User-entered deadlines are device-local and keyed by the authenticated Supabase UUID.
 * A weekday-only school schedule is not sufficient to synthesize dated class meetings;
 * sessions remain empty until the university publishes an authenticated calendar.
 */
@Singleton
class LocalAccountTimetableRepository @Inject constructor(
    @ApplicationContext context: Context,
    private val session: SessionRepository,
    private val client: SupabaseRestClient,
) : TimetableRepository {
    private val prefs = context.getSharedPreferences("uth_user_deadlines", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }
    private val guard = Any()
    private var activeUser: String? = null
    private val state = MutableStateFlow(Timetable())
    override val timetable = state.asStateFlow()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    init {
        scope.launch {
            session.session.collect { current ->
                synchronized(guard) {
                    val userId = if (current.profile != null) runCatching { uuid(client.userId) }.getOrNull() else null
                    if (activeUser != userId) {
                        activeUser = userId
                        state.value = Timetable(deadlines = userId?.let(::readDeadlines).orEmpty())
                    }
                }
            }
        }
    }

    override fun addDeadline(course: Course, title: String, dueAt: LocalDateTime) {
        require(title.isNotBlank())
        synchronized(guard) {
            val userId = runCatching { verifiedActiveUser() }.getOrNull() ?: return
            val entries = state.value.deadlines + Deadline(UUID.randomUUID().toString(), course, title.trim(), dueAt)
            runCatching { saveDeadlines(userId, entries) }
            state.update { it.copy(deadlines = entries) }
        }
    }

    override fun completeDeadline(id: String) {
        synchronized(guard) {
            val userId = runCatching { verifiedActiveUser() }.getOrNull() ?: return
            val entries = state.value.deadlines.map { if (it.id == id) it.copy(completed = true) else it }
            runCatching { saveDeadlines(userId, entries) }
            state.update { it.copy(deadlines = entries) }
        }
    }

    private fun verifiedActiveUser(): String {
        check(session.session.value.profile != null) { "Sign in to manage personal deadlines" }
        val actualUser = uuid(client.userId)
        check(activeUser == actualUser) { "Account deadlines are still loading" }
        return actualUser
    }

    private fun readDeadlines(userId: String): List<Deadline> = try {
        val data = prefs.getString("user_$userId", null) ?: return emptyList()
        json.parseToJsonElement(data).jsonArray.map { item ->
            val row = item.jsonObject
            Deadline(
                id = UUID.fromString(row.getValue("id").jsonPrimitive.content).toString(),
                course = Course.valueOf(row.getValue("course").jsonPrimitive.content),
                title = row.getValue("title").jsonPrimitive.content,
                dueAt = LocalDateTime.parse(row.getValue("due_at").jsonPrimitive.content),
                completed = row["completed"]?.jsonPrimitive?.booleanOrNull == true,
            )
        }
    } catch (_: Exception) {
        // Corrupt local input must never display another user's or fabricated school data.
        emptyList()
    }

    private fun saveDeadlines(userId: String, entries: List<Deadline>) {
        val value = JsonArray(
            entries.map { item ->
                buildJsonObject {
                    put("id", item.id)
                    put("course", item.course.name)
                    put("title", item.title)
                    put("due_at", item.dueAt.toString())
                    put("completed", item.completed)
                }
            },
        ).toString()
        check(prefs.edit().putString("user_$userId", value).commit()) { "Cannot save deadline" }
    }
}
