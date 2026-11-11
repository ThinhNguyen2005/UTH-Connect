package com.giathinh.uthconnect.domain.model

/**
 * Tọa độ chính xác, địa chỉ đầy đủ và kênh liên hệ của tin trọ — tách khỏi
 * [HousingListing] vì dòng public của feed không được mang PII: RLS chỉ cho chủ
 * tin đọc thẳng, người khác nhận qua RPC reveal sau contact request ACCEPTED.
 */
data class HousingPrivateDetails(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val exactAddress: String = "",
    val contactPhone: String = "",
    val contactZaloUrl: String = "",
)
