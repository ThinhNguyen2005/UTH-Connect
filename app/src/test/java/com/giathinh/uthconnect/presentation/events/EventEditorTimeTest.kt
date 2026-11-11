package com.giathinh.uthconnect.presentation.events

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Kiểm tra phần quy đổi thời gian của hộp thoại tạo/sửa sự kiện.
 * Nhắm đúng lỗi đã xảy ra: mặc định "bây giờ + 24 giờ" khiến người tạo chỉ sửa giờ,
 * quên sửa ngày, sự kiện rơi sang hôm sau và mọi lần quét đều báo CHECKIN_NOT_OPEN.
 */
class EventEditorTimeTest {
    private val zone = ZoneId.of("Asia/Ho_Chi_Minh")

    @Test
    fun `mac dinh su kien moi bat dau sau khoang 1 gio chu khong phai 24 gio`() {
        val (start, end) = defaultEventWindow(zone)
        // Không so ngày "hôm nay": chạy sau 23h thì now+60 phút vắt sang ngày mai
        // là hợp lệ — điều cần chặn là mặc định +24 giờ của bug cũ.
        val leadMinutes = Duration.between(LocalDateTime.now(zone), start).toMinutes()
        assertTrue("start phải cách hiện tại ~1 giờ, thực tế $leadMinutes phút", leadMinutes in 55L..65L)
        assertEquals(120L, Duration.between(start, end).toMinutes())
    }

    @Test
    fun `mac dinh bat dau o tuong lai de create_event khong tu choi`() {
        val (start, _) = defaultEventWindow(zone)
        val now = LocalDateTime.now(zone)
        assertTrue("start must stay in the future", start.isAfter(now))
        assertTrue("start must leave a one minute lead", Duration.between(now, start).toMinutes() >= 55L)
    }

    @Test
    fun `mac dinh lam tron len boi so nam phut`() {
        val (start, _) = defaultEventWindow(zone)
        assertEquals(0, start.minute % 5)
        assertEquals(0, start.second)
    }

    @Test
    fun `parse chap nhan dinh dang ngay gio va tu choi chuoi sai`() {
        assertEquals(LocalDateTime.of(2026, 11, 4, 20, 20), parseEditableDateTime("04/11/2026 20:20"))
        assertNull(parseEditableDateTime("20:20 04/11/2026"))
        assertNull(parseEditableDateTime("04/11/2026"))
        assertNull(parseEditableDateTime(""))
    }

    @Test
    fun `editableDateTime tra ve gio Viet Nam ma nguoi dung nhap`() {
        val millis = LocalDateTime.of(2026, 11, 4, 20, 20).atZone(zone).toInstant().toEpochMilli()
        assertEquals("04/11/2026 20:20", editableDateTime(millis, zone))
    }

    @Test
    fun `mo editor cua su kien cu khong lam dich gia tri da luu`() {
        val stored = LocalDateTime.of(2026, 11, 5, 13, 20).atZone(ZoneId.of("UTC")).toInstant().toEpochMilli()
        assertEquals(LocalDateTime.of(2026, 11, 5, 20, 20), parseEditableDateTime(editableDateTime(stored, zone)))
    }
}
