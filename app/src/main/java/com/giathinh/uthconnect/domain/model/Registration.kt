package com.giathinh.uthconnect.domain.model

import java.time.LocalDateTime

/** Model chuẩn bị cho bước đăng ký thật; task này chưa tạo vé hay ghi dữ liệu. */
data class Registration(
    val id: String,
    val eventId: String,
    val studentId: String,
    val registeredAt: LocalDateTime,
)
