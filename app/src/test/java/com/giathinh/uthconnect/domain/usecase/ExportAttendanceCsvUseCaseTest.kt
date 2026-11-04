package com.giathinh.uthconnect.domain.usecase

import com.giathinh.uthconnect.domain.model.AttendanceEntry
import com.giathinh.uthconnect.domain.model.AttendanceRoster
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExportAttendanceCsvUseCaseTest {
    private val labels = AttendanceCsvLabels(
        "Sự kiện", "Xuất lúc", "MSSV", "Họ tên", "Email", "Trạng thái", "Giờ vào",
        "Người điểm danh", "Đã điểm danh", "Chưa điểm danh",
    )

    @Test fun `CSV includes all registered students and local check-in time`() {
        val roster = AttendanceRoster("e", "Hội thao", listOf(
            AttendanceEntry("1", "01", "An", "a@ut.edu.vn", "2026-11-03T01:00:00Z", "organizer"),
            AttendanceEntry("2", "02", "Bình", "b@ut.edu.vn", null, null),
        ))
        val csv = ExportAttendanceCsvUseCase()(roster, labels, Instant.parse("2026-11-03T00:00:00Z"))
        assertEquals(6, csv.trimEnd().split("\r\n").size)
        assertTrue(csv.contains("03/11/2026 08:00:00"))
        assertTrue(csv.contains("\"Chưa điểm danh\""))
    }

    @Test fun `CSV escapes quotes and blocks spreadsheet formulas`() {
        val roster = AttendanceRoster("e", "Event", listOf(
            AttendanceEntry("1", "01", " =HYPERLINK(\"x\")", "a@ut.edu.vn", null, null),
        ))
        val csv = ExportAttendanceCsvUseCase()(roster, labels, Instant.EPOCH)
        assertTrue(csv.contains("\"' =HYPERLINK(\"\"x\"\")\""))
    }
}
