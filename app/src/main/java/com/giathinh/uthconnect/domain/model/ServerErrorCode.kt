package com.giathinh.uthconnect.domain.model

import java.io.IOException

/**
 * Hợp đồng mã lỗi của server: mỗi giá trị ứng với một `raise exception '<CODE>'` trong
 * `supabase/migrations`, đi tới app qua `SupabaseHttpException.message`.
 *
 * Server cố tình phát mã ngắn thay vì câu có người đọc được (SECURITY DEFINER function
 * không nên nhét chuỗi giao diện). Vì vậy lớp domain chỉ nhận diện mã, còn câu dịch tiếng
 * Việt thuộc về `presentation/common/ServerErrorMessage.kt` để tôn trọng tính đa ngữ.
 */
enum class ServerErrorCode(val token: String) {
    // Vé và điểm danh
    InvalidQr("INVALID_QR"),
    QrExpired("QR_EXPIRED"),
    CheckInNotOpen("CHECKIN_NOT_OPEN"),
    AlreadyCheckedIn("ALREADY_CHECKED_IN"),
    NotRegistered("NOT_REGISTERED"),
    TicketUnavailable("TICKET_UNAVAILABLE"),
    StudentNotVerified("STUDENT_NOT_VERIFIED"),

    // Sự kiện và đăng ký
    EventNotFound("EVENT_NOT_FOUND"),
    EventFull("EVENT_FULL"),
    RegistrationClosed("REGISTRATION_CLOSED"),
    EventLocked("EVENT_LOCKED"),
    EventAlreadyStarted("EVENT_ALREADY_STARTED"),
    InvalidEvent("INVALID_EVENT"),
    InvalidEventStatus("INVALID_EVENT_STATUS"),
    InvalidEventTransition("INVALID_EVENT_TRANSITION"),

    // Hồ sơ sinh viên và phân quyền
    InvalidStudentId("INVALID_STUDENT_ID"),
    StudentIdAlreadyVerified("STUDENT_ID_ALREADY_VERIFIED"),
    UserNotFound("USER_NOT_FOUND"),
    AdminCannotDemoteSelf("ADMIN_CANNOT_DEMOTE_SELF"),
    Forbidden("FORBIDDEN"),
    AuthRequired("AUTH_REQUIRED"),

    /** Không có mã nào của server trong lỗi — rơi về hai giá trị dưới. */
    Network(""),
    Unknown("");

    companion object {
        // Ưu tiên token dài hơn để INVALID_EVENT_STATUS không bị đọc nhầm thành INVALID_EVENT.
        private val byTokenLength = entries.sortedByDescending { it.token.length }

        /** Nhận diện mã từ chuỗi lỗi server; trả null khi chuỗi không chứa mã nào đã biết. */
        fun fromMessage(text: String?): ServerErrorCode? {
            if (text.isNullOrBlank()) return null
            val normalized = text.uppercase()
            return byTokenLength.firstOrNull { it.token.isNotEmpty() && normalized.contains(it.token) }
        }

        /**
         * Mã hoá một `Throwable` bất kỳ từ tầng data: ưu tiên mã server, rồi lỗi mạng,
         * cuối cùng là `Unknown` để giao diện còn có đường fallback về chuỗi gốc.
         */
        fun of(error: Throwable?): ServerErrorCode =
            fromMessage(error?.message) ?: if (error is IOException) Network else Unknown
    }
}
