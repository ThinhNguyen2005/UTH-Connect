package com.giathinh.uthconnect.data.repository

import com.giathinh.uthconnect.domain.model.CourseSchedule
import com.giathinh.uthconnect.domain.repository.ScheduleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

/**
 * Fake/Static implementation của [ScheduleRepository] dùng để UI team
 * (KAN-31 – Văn Tú Huy Thiên) có thể lấy dữ liệu ngay mà không cần API thật.
 *
 * Dữ liệu giả lập cho học kỳ 1 năm học 2025-2026.
 * Thay thế bằng [FirestoreScheduleRepository] khi backend sẵn sàng.
 */
class FakeScheduleRepository @Inject constructor() : ScheduleRepository {

    // -----------------------------------------------------------------------
    // Sample data – Lịch học mẫu HK1 2025-2026
    // -----------------------------------------------------------------------
    private val sampleSchedule: List<CourseSchedule> = listOf(
        // Thứ 2
        CourseSchedule(
            id = "sch-001",
            courseCode = "ENG201",
            courseName = "Tiếng Anh Chuyên Ngành",
            lecturerName = "ThS. Nguyễn Thị Lan",
            room = "B102",
            campus = "Quang Trung",
            dayOfWeek = 2,
            startPeriod = 1,
            endPeriod = 3,
            startTime = "07:00",
            endTime = "09:30",
            date = "2026-11-03",
            credits = 3,
        ),
        CourseSchedule(
            id = "sch-002",
            courseCode = "INT301",
            courseName = "Nhập môn Trí tuệ Nhân tạo",
            lecturerName = "TS. Trần Văn Hùng",
            room = "A205",
            campus = "Quang Trung",
            dayOfWeek = 2,
            startPeriod = 4,
            endPeriod = 6,
            startTime = "09:45",
            endTime = "12:15",
            date = "2026-11-03",
            credits = 3,
        ),

        // Thứ 3
        CourseSchedule(
            id = "sch-003",
            courseCode = "MTH201",
            courseName = "Toán Rời Rạc",
            lecturerName = "TS. Lê Thị Hoa",
            room = "C301",
            campus = "Quang Trung",
            dayOfWeek = 3,
            startPeriod = 1,
            endPeriod = 3,
            startTime = "07:00",
            endTime = "09:30",
            date = "2026-11-04",
            credits = 3,
        ),

        // Thứ 4
        CourseSchedule(
            id = "sch-004",
            courseCode = "DSA201",
            courseName = "Cấu trúc Dữ liệu & Giải thuật",
            lecturerName = "ThS. Phạm Minh Tuấn",
            room = "A203",
            campus = "Quang Trung",
            dayOfWeek = 4,
            startPeriod = 4,
            endPeriod = 6,
            startTime = "09:45",
            endTime = "12:15",
            date = "2026-11-05",
            credits = 4,
        ),

        // Thứ 5
        CourseSchedule(
            id = "sch-005",
            courseCode = "AND301",
            courseName = "Lập trình Android",
            lecturerName = "ThS. Nguyễn Tiến Dũng",
            room = "Lab 3",
            campus = "Nguyễn Tất Thành",
            dayOfWeek = 5,
            startPeriod = 7,
            endPeriod = 9,
            startTime = "13:00",
            endTime = "15:30",
            date = "2026-11-06",
            credits = 3,
        ),
        CourseSchedule(
            id = "sch-006",
            courseCode = "SEC201",
            courseName = "An toàn Thông tin",
            lecturerName = "TS. Hoàng Văn Nam",
            room = "B204",
            campus = "Nguyễn Tất Thành",
            dayOfWeek = 5,
            startPeriod = 10,
            endPeriod = 12,
            startTime = "15:45",
            endTime = "18:15",
            date = "2026-11-06",
            credits = 3,
        ),

        // Thứ 6
        CourseSchedule(
            id = "sch-007",
            courseCode = "DB301",
            courseName = "Cơ sở Dữ liệu Nâng cao",
            lecturerName = "TS. Vũ Thị Thu",
            room = "Lab 1",
            campus = "Quang Trung",
            dayOfWeek = 6,
            startPeriod = 1,
            endPeriod = 3,
            startTime = "07:00",
            endTime = "09:30",
            date = "2026-11-07",
            credits = 4,
        ),

        // Thứ 7
        CourseSchedule(
            id = "sch-008",
            courseCode = "SE301",
            courseName = "Công nghệ Phần mềm",
            lecturerName = "ThS. Bùi Quang Hiếu",
            room = "A101",
            campus = "Thủ Đức",
            dayOfWeek = 7,
            startPeriod = 4,
            endPeriod = 6,
            startTime = "09:45",
            endTime = "12:15",
            date = "2026-11-08",
            credits = 3,
        ),
    )

    // -----------------------------------------------------------------------
    // Interface implementation
    // -----------------------------------------------------------------------

    /** Trả về lịch học trong ngày [date] (định dạng "YYYY-MM-DD"). */
    override fun getScheduleByDate(date: String): Flow<List<CourseSchedule>> =
        flowOf(sampleSchedule.filter { it.date == date })

    /** Trả về tất cả môn học trong [dayOfWeek] (2=Thứ 2 … 8=Chủ nhật). */
    override fun getScheduleByDayOfWeek(dayOfWeek: Int): Flow<List<CourseSchedule>> =
        flowOf(sampleSchedule.filter { it.dayOfWeek == dayOfWeek })

    /** Trả về toàn bộ thời khoá biểu học kỳ hiện tại. */
    override fun getSemesterSchedule(): Flow<List<CourseSchedule>> =
        flowOf(sampleSchedule)
}
