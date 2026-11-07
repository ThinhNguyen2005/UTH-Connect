package com.giathinh.uthconnect.domain.repository

import com.giathinh.uthconnect.domain.model.Event
import com.giathinh.uthconnect.domain.model.EventDraft
import com.giathinh.uthconnect.domain.model.EventStatus
import kotlinx.coroutines.flow.Flow

/**
 * Kho dữ liệu Sự kiện sinh viên (kết nối Firestore).
 */
interface EventRepository {
    /** Lấy danh sách sự kiện, có thể lọc theo cơ sở (campus). */
    fun getEvents(campus: String? = null): Flow<List<Event>>

    /** Lấy thông tin chi tiết một sự kiện. */
    suspend fun getEventDetail(eventId: String): Result<Event?>

    /** Đăng ký tham gia sự kiện (thực hiện qua transaction chống vượt số lượng). */
    suspend fun registerEvent(
        eventId: String,
        uid: String,
        studentId: String,
        studentName: String,
    ): Result<Unit>

    /** Hủy đăng ký sự kiện. */
    suspend fun cancelRegistration(eventId: String, uid: String): Result<Unit>

    /** Kiểm tra xem sinh viên hiện tại đã đăng ký vé cho sự kiện này chưa. */
    fun isRegistered(eventId: String, uid: String): Flow<Boolean>

    /** Supabase's authenticated RPCs derive user identity from the JWT. */
    suspend fun registerCurrentUser(eventId: String): Result<Unit> = Result.failure(UnsupportedOperationException("Authenticated event registration unavailable"))

    suspend fun cancelCurrentRegistration(eventId: String): Result<Unit> = Result.failure(UnsupportedOperationException("Authenticated cancellation unavailable"))

    fun currentUserRegistrations(): Flow<Set<String>> = kotlinx.coroutines.flow.flowOf(emptySet())

    fun refresh() = Unit

    fun currentUserId(): String? = null

    /** Saves a new or existing event and returns its persisted ID. */
    suspend fun saveEvent(eventId: String?, draft: EventDraft): Result<String> = Result.failure(UnsupportedOperationException("Event management unavailable"))

    suspend fun setStatus(eventId: String, status: EventStatus): Result<Unit> = Result.failure(UnsupportedOperationException("Event management unavailable"))
}
