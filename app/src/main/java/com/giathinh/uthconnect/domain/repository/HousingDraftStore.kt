package com.giathinh.uthconnect.domain.repository

/**
 * Nháp form đăng tin Ghép trọ, lưu cục bộ theo từng tài khoản trên thiết bị
 * (tắt app mở lại còn; tài khoản khác không đọc/ghi đè/xóa nháp của nhau).
 * Payload là chuỗi JSON do presentation tự serialize — data layer không biết
 * cấu trúc form nên không cần migration mỗi lần đổi trường.
 */
interface HousingDraftStore {
    suspend fun read(owner: String): String?
    suspend fun write(owner: String, payload: String)
    suspend fun clear(owner: String)
}
