package com.giathinh.uthconnect.domain.repository

import com.giathinh.uthconnect.domain.model.Event
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
}
