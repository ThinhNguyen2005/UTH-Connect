package com.giathinh.uthconnect.domain.model

/** Vé đăng ký của một sinh viên cho một sự kiện. */
data class Ticket(
    val id: String,
    val studentId: String,
    val eventId: String,
    val eventTitle: String,
    val eventLocation: String,
    val eventStartsAt: String,
    val status: TicketStatus,
    val fallbackCode: String,
    /** Đơn vị tổ chức in dưới tên vé; null khi sự kiện chưa có dữ liệu này. */
    val organizerName: String? = null,
    /** Chỗ ngồi in trên vé; null với sự kiện không chia ghế. */
    val seatLabel: String? = null,
)

/** Trạng thái nghiệp vụ của vé. */
enum class TicketStatus { VALID, CHECKED_IN, CANCELLED }

/** Nội dung QR của vé demo; nonce làm mới hình mỗi 60 giây, không phải chữ ký bảo mật. */
object TicketQrPayload {
    private const val PREFIX = "UTHCONNECT:TICKET:1:"

    fun create(ticketId: String, nonce: Long): String {
        require(ticketId.isNotBlank() && ':' !in ticketId)
        return "$PREFIX$ticketId:$nonce"
    }

    fun ticketIdOrNull(raw: String): String? {
        if (!raw.startsWith(PREFIX)) return null
        val body = raw.removePrefix(PREFIX)
        val id = body.substringBefore(':', missingDelimiterValue = "")
        val nonce = body.substringAfter(':', missingDelimiterValue = "").toLongOrNull()
        return id.takeIf { it.isNotBlank() && ':' !in it && nonce != null && nonce >= 0 }
    }
}
