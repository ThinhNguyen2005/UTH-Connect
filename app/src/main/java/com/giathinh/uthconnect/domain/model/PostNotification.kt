package com.giathinh.uthconnect.domain.model

/**
 * Data Model Bảng tin & Thông báo Nhà trường.
 * Phục vụ module KAN-28 (Đức Dịch Viết).
 */
data class PostNotification(
    val id: String,
    val title: String,
    val summary: String,
    val content: String,
    val category: NotificationCategory,
    val author: String, // "Phòng Đào tạo", "Đoàn - Hội UTH", "Phòng Công tác SV"...
    val publishedAt: Long, // Epoch timestamp (ms)
    val isPinned: Boolean = false,
    val attachmentUrl: String? = null,
    val imageUrl: String? = null,
)

enum class NotificationCategory {
    ALL,
    ACADEMIC,     // Học vụ & Đào tạo
    EXAMINATION,  // Lịch thi & Khảo thí
    TUITION,      // Học phí & Học bổng
    ACTIVITIES,   // Hoạt động & Phong trào
    GENERAL;      // Thông báo chung

    companion object {
        fun fromString(value: String?): NotificationCategory {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: ALL
        }
    }
}
