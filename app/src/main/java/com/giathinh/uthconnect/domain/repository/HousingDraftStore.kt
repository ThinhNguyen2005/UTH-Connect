package com.giathinh.uthconnect.domain.repository

/**
 * Nháp form đăng tin Ghép trọ, lưu cục bộ theo thiết bị (tắt app mở lại còn).
 * Payload là chuỗi JSON do presentation tự serialize — data layer không biết
 * cấu trúc form nên không cần migration mỗi lần đổi trường.
 */
interface HousingDraftStore {
    suspend fun read(): String?
    suspend fun write(payload: String)
    suspend fun clear()
}
