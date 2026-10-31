package com.giathinh.uthconnect.domain.model

import java.time.LocalDateTime

/** Sáu nội dung mẫu. UI dịch tên, mô tả và địa điểm bằng stringResource. */
enum class EventTopic {
    CAREER_DAY, ANDROID_WORKSHOP, ENGLISH_CLUB, GREEN_CAMPUS, LOGISTICS_TALK, WELCOME_FRESHMEN,
}

data class Event(
    val id: String,
    val topic: EventTopic,
    val campus: Campus,
    val startAt: LocalDateTime,
    val endAt: LocalDateTime,
    val capacity: Int,
    val registeredCount: Int,
    val status: EventStatus,
) {
    init {
        require(capacity > 0) { "Số chỗ phải lớn hơn 0" }
        require(registeredCount in 0..capacity) { "Số người đăng ký phải nằm trong sức chứa" }
        require(endAt > startAt) { "Giờ kết thúc phải sau giờ bắt đầu" }
    }

    val remainingSeats: Int get() = capacity - registeredCount

    // Dùng cùng một điều kiện cho UI và điều hướng, tránh mỗi nơi kiểm tra một kiểu.
    val canRegister: Boolean get() = status == EventStatus.OPEN && remainingSeats > 0
}
