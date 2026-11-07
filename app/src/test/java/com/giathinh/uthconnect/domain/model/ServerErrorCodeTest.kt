package com.giathinh.uthconnect.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.IOException

/**
 * Kiểm tra bộ ánh xạ chuỗi lỗi Postgres -> mã xử lý được ở giao diện.
 * Danh sách token lấy từ `raise exception` trong `supabase/migrations`, nên nếu server
 * thêm mã mới mà app chưa biết, test đầu tiên sẽ bắt được sự lệch hợp đồng.
 */
class ServerErrorCodeTest {
    private val contract = listOf(
        "INVALID_QR" to ServerErrorCode.InvalidQr,
        "QR_EXPIRED" to ServerErrorCode.QrExpired,
        "CHECKIN_NOT_OPEN" to ServerErrorCode.CheckInNotOpen,
        "ALREADY_CHECKED_IN" to ServerErrorCode.AlreadyCheckedIn,
        "NOT_REGISTERED" to ServerErrorCode.NotRegistered,
        "TICKET_UNAVAILABLE" to ServerErrorCode.TicketUnavailable,
        "STUDENT_NOT_VERIFIED" to ServerErrorCode.StudentNotVerified,
        "EVENT_NOT_FOUND" to ServerErrorCode.EventNotFound,
        "EVENT_FULL" to ServerErrorCode.EventFull,
        "REGISTRATION_CLOSED" to ServerErrorCode.RegistrationClosed,
        "EVENT_LOCKED" to ServerErrorCode.EventLocked,
        "EVENT_ALREADY_STARTED" to ServerErrorCode.EventAlreadyStarted,
        "INVALID_EVENT" to ServerErrorCode.InvalidEvent,
        "INVALID_EVENT_STATUS" to ServerErrorCode.InvalidEventStatus,
        "INVALID_EVENT_TRANSITION" to ServerErrorCode.InvalidEventTransition,
        "INVALID_STUDENT_ID" to ServerErrorCode.InvalidStudentId,
        "STUDENT_ID_ALREADY_VERIFIED" to ServerErrorCode.StudentIdAlreadyVerified,
        "USER_NOT_FOUND" to ServerErrorCode.UserNotFound,
        "ADMIN_CANNOT_DEMOTE_SELF" to ServerErrorCode.AdminCannotDemoteSelf,
        "FORBIDDEN" to ServerErrorCode.Forbidden,
        "AUTH_REQUIRED" to ServerErrorCode.AuthRequired,
    )

    @Test
    fun `toan bo ma server trong hop dong deu duoc nhan dien`() {
        contract.forEach { (token, expected) ->
            assertEquals(token, expected, ServerErrorCode.fromMessage(token))
        }
    }

    @Test
    fun `ma dai khong bi ma ngan che mat`() {
        assertEquals(ServerErrorCode.InvalidEventStatus, ServerErrorCode.fromMessage("INVALID_EVENT_STATUS"))
        assertEquals(
            ServerErrorCode.InvalidEventTransition,
            ServerErrorCode.fromMessage("INVALID_EVENT_TRANSITION"),
        )
        assertEquals(
            ServerErrorCode.EventAlreadyStarted,
            ServerErrorCode.fromMessage("EVENT_ALREADY_STARTED"),
        )
        assertEquals(ServerErrorCode.AlreadyCheckedIn, ServerErrorCode.fromMessage("ALREADY_CHECKED_IN"))
        assertEquals(
            ServerErrorCode.StudentIdAlreadyVerified,
            ServerErrorCode.fromMessage("STUDENT_ID_ALREADY_VERIFIED"),
        )
    }

    @Test
    fun `van thay ma trong chuoi loi dai cua postgrest`() {
        val verbose = "ERROR: CHECKIN_NOT_OPEN\nCONTEXT: " +
            "PL/pgSQL function public.check_in_ticket_qr(text) line 12 at RAISE"
        assertEquals(ServerErrorCode.CheckInNotOpen, ServerErrorCode.fromMessage(verbose))
    }

    @Test
    fun `khong phan biet hoa thuong`() {
        assertEquals(ServerErrorCode.NotRegistered, ServerErrorCode.fromMessage("not_registered"))
    }

    @Test
    fun `loi mang duoc phan biet voi loi server`() {
        assertEquals(ServerErrorCode.Network, ServerErrorCode.of(IOException("Unable to resolve host")))
    }

    @Test
    fun `loi khong nhan dien duoc thi ve unknown`() {
        val illegal = IllegalStateException("Invalid student session")
        assertEquals(ServerErrorCode.Unknown, ServerErrorCode.of(illegal))
        assertEquals(ServerErrorCode.Unknown, ServerErrorCode.of(null))
        assertEquals(ServerErrorCode.Unknown, ServerErrorCode.of(RuntimeException("")))
    }

    @Test
    fun `chuoi null duoc bao ve o fromMessage`() {
        assertNull(ServerErrorCode.fromMessage(null))
        assertNull(ServerErrorCode.fromMessage("   "))
        assertNull(ServerErrorCode.fromMessage("nothing to see here"))
    }
}
