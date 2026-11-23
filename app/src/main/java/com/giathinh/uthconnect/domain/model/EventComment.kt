package com.giathinh.uthconnect.domain.model

/**
 * Mô hình dữ liệu bình luận sự kiện của sinh viên UTH.
 */
data class EventComment(
    val id: String,
    val eventId: String,
    val authorId: String,
    val authorName: String,
    val authorStudentId: String = "",
    val authorClass: String = "",
    val authorMajor: String = "",
    val authorAvatarUrl: String? = null,
    val content: String,
    val createdAt: Long = System.currentTimeMillis(),
)
