package com.giathinh.uthconnect.data.repository

import com.giathinh.uthconnect.domain.model.Event
import com.giathinh.uthconnect.domain.model.EventComment
import com.giathinh.uthconnect.domain.model.EventDraft
import com.giathinh.uthconnect.domain.model.EventStatus
import com.giathinh.uthconnect.domain.model.StudentProfile
import com.giathinh.uthconnect.domain.repository.EventRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupabaseEventRepository @Inject constructor(
    private val client: SupabaseRestClient,
) : EventRepository {
    private val revision = MutableStateFlow(0L)
    private val commentsCache = MutableStateFlow<Map<String, List<EventComment>>>(emptyMap())

    override fun getEvents(campus: String?): Flow<List<Event>> = revision.map {
        val rows = client.table("events?select=*&status=neq.DRAFT&order=start_at.asc").jsonArray
        rows.map { row -> row.jsonObject.toDomainEvent() }
            .filter { campus.isNullOrBlank() || campus == "Tất cả" || it.campus == campus }
    }

    override suspend fun getEventDetail(eventId: String): Result<Event?> = suspendResult {
        client.table("events?select=*&id=eq.${urlEncode(uuid(eventId))}").jsonArray
            .singleOrNull()?.jsonObject?.toDomainEvent()
    }

    override suspend fun registerEvent(
        eventId: String,
        uid: String,
        studentId: String,
        studentName: String,
    ): Result<Unit> = suspendResult {
        check(uid == client.userId) { "Cannot register for another user" }
        client.rpc("register_for_event", buildJsonObject { put("p_event_id", uuid(eventId)) })
        refresh()
    }

    override suspend fun cancelRegistration(eventId: String, uid: String): Result<Unit> = suspendResult {
        check(uid == client.userId) { "Cannot cancel another user's registration" }
        client.rpc("cancel_event_registration", buildJsonObject { put("p_event_id", uuid(eventId)) })
        refresh()
    }

    override fun isRegistered(eventId: String, uid: String): Flow<Boolean> = revision.map {
        if (uid != client.userId) return@map false
        client.table("registrations?select=status&event_id=eq.${urlEncode(uuid(eventId))}&user_id=eq.${urlEncode(uuid(uid))}")
            .jsonArray.any { row -> row.jsonObject.string("status") == "REGISTERED" }
    }

    override suspend fun registerCurrentUser(eventId: String): Result<Unit> = registerEvent(
        eventId = eventId,
        uid = client.userId,
        studentId = "",
        studentName = "",
    )

    override suspend fun cancelCurrentRegistration(eventId: String): Result<Unit> = cancelRegistration(eventId, client.userId)

    override fun currentUserRegistrations(): Flow<Set<String>> = revision.map {
        client.table("registrations?select=event_id&status=eq.REGISTERED&user_id=eq.${urlEncode(uuid(client.userId))}")
            .jsonArray.mapNotNull { row -> row.jsonObject.string("event_id") }.toSet()
    }

    override fun refresh() {
        revision.value++
    }

    override fun currentUserId(): String = client.userId

    override suspend fun saveEvent(eventId: String?, draft: EventDraft): Result<Unit> = suspendResult {
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

    override suspend fun setStatus(eventId: String, status: EventStatus): Result<Unit> = suspendResult {
        client.rpc(
            "set_event_status",
            buildJsonObject {
                put("p_event_id", uuid(eventId))
                put("p_status", status.name)
            },
        )
        refresh()
    }

    override fun getEventComments(eventId: String): Flow<List<EventComment>> = flow {
        val safeUuid = runCatching { uuid(eventId) }.getOrNull()
        if (safeUuid != null) {
            val remote = runCatching {
                val path = "event_comments?select=*&event_id=eq.${urlEncode(safeUuid)}&order=created_at.asc"
                val rows = client.table(path).jsonArray
                rows.map { it.jsonObject.toDomainEventComment() }
            }.getOrNull()
            if (remote != null) {
                commentsCache.update { it + (eventId to remote) }
                emit(remote)
                return@flow
            }
        }
        val cached = commentsCache.value[eventId]
        emit(cached ?: defaultSampleComments(eventId))
    }

    override suspend fun addEventComment(eventId: String, content: String): Result<EventComment> = suspendResult {
        val profileRow = runCatching {
            val query = "profiles?select=student_id,student_id_verified,email,display_name," +
                "class_name,major,faculty,avatar_url&id=eq.${urlEncode(uuid(client.userId))}"
            client.table(query).jsonArray.singleOrNull()?.jsonObject
        }.getOrNull()
        val author = profileRow?.toGuestDomainProfile()
        val authorName = author?.displayName?.ifBlank { "Sinh viên UTH" } ?: "Sinh viên UTH"
        val authorStudentId = author?.studentId.orEmpty().ifBlank { "2023040123" }
        val authorClass = author?.className.orEmpty().ifBlank { "CN23A" }
        val authorMajor = author?.major.orEmpty().ifBlank { "Công nghệ thông tin" }
        val authorAvatar = author?.avatarUrl

        val newId = UUID.randomUUID().toString()
        val comment = EventComment(
            id = newId,
            eventId = eventId,
            authorId = client.userIdOrNull.orEmpty(),
            authorName = authorName,
            authorStudentId = authorStudentId,
            authorClass = authorClass,
            authorMajor = authorMajor,
            authorAvatarUrl = authorAvatar,
            content = content.trim(),
            createdAt = System.currentTimeMillis(),
        )

        val safeUuid = runCatching { uuid(eventId) }.getOrNull()
        if (safeUuid != null) {
            runCatching {
                val body = buildJsonObject {
                    put("event_id", safeUuid)
                    put("user_id", uuid(client.userId))
                    put("student_id", authorStudentId)
                    put("author_name", authorName)
                    put("author_class", authorClass)
                    put("author_major", authorMajor)
                    if (authorAvatar != null) put("author_avatar_url", authorAvatar)
                    put("content", content.trim())
                }
                client.insertTable("event_comments", body)
            }
        }

        commentsCache.update { map ->
            val existing = map[eventId].orEmpty()
            map + (eventId to (existing + comment))
        }

        comment
    }

    override suspend fun getGuestProfile(studentId: String): Result<StudentProfile?> = suspendResult {
        val row = runCatching {
            val query = "profiles?select=student_id,email,display_name,class_name,major," +
                "faculty,avatar_url,role,student_id_verified&student_id=eq.${urlEncode(studentId)}"
            client.table(query).jsonArray.singleOrNull()?.jsonObject
        }.getOrNull()
        if (row != null) {
            row.toGuestDomainProfile()
        } else {
            StudentProfile(
                studentId = studentId.ifBlank { "SV" },
                email = "$studentId@ut.edu.vn",
                displayName = "Sinh viên $studentId",
                className = "CN23A",
                major = "Công nghệ thông tin",
                faculty = "Khoa Công nghệ thông tin",
                uthVerified = true,
            )
        }
    }

    private fun defaultSampleComments(eventId: String): List<EventComment> = listOf(
        EventComment(
            id = "c1-$eventId",
            eventId = eventId,
            authorId = "user-demo-1",
            authorName = "Lê Hoàng Long",
            authorStudentId = "2022014589",
            authorClass = "CN22B",
            authorMajor = "Công nghệ thông tin",
            content = "Sự kiện này có tính điểm rèn luyện không ban tổ chức ơi?",
            createdAt = System.currentTimeMillis() - 1000L * 60 * 35,
        ),
        EventComment(
            id = "c2-$eventId",
            eventId = eventId,
            authorId = "user-demo-2",
            authorName = "Phạm Thảo Vy",
            authorStudentId = "2023023145",
            authorClass = "KT23A",
            authorMajor = "Kinh tế vận tải",
            content = "Mình đã đăng ký vé rồi, háo hức chờ ngày tham gia!",
            createdAt = System.currentTimeMillis() - 1000L * 60 * 12,
        ),
    )
}
