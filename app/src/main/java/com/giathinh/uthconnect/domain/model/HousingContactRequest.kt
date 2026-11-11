package com.giathinh.uthconnect.domain.model

/** Vòng đời một yêu cầu liên hệ — trùng enum `housing_contact_status` phía Supabase. */
enum class HousingContactStatus {
    PENDING,
    ACCEPTED,
    REJECTED,
    CANCELLED,
}

/**
 * Yêu cầu mở khóa thông tin liên hệ của một tin. `requesterName` chỉ có khi đọc
 * ở phía chủ tin (embed profiles); phía người hỏi không cần tên chính mình.
 */
data class HousingContactRequest(
    val id: String = "",
    val listingId: String = "",
    val requesterId: String = "",
    val requesterName: String = "",
    val status: HousingContactStatus = HousingContactStatus.PENDING,
    val message: String = "",
    val createdAtMillis: Long = 0L,
)
