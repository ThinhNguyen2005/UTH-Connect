package com.giathinh.uthconnect.domain.repository

import com.giathinh.uthconnect.domain.model.CourseSchedule
import kotlinx.coroutines.flow.Flow

/**
 * Interface Repository quản lý Lịch học & Thời khóa biểu.
 * Phục vụ KAN-31 (Văn Tạ Huy Thiên).
 */
interface ScheduleRepository {
    /** Lấy lịch học theo ngày cụ thể (định dạng "YYYY-MM-DD", ví dụ: "2026-11-02") */
    fun getScheduleByDate(date: String): Flow<List<CourseSchedule>>

    /** Lấy thời khóa biểu theo thứ trong tuần (2 = Thứ Hai, ..., 8 = Chủ Nhật) */
    fun getScheduleByDayOfWeek(dayOfWeek: Int): Flow<List<CourseSchedule>>

    /** Lấy toàn bộ thời khóa biểu trong học kỳ hiện tại */
    fun getSemesterSchedule(): Flow<List<CourseSchedule>>
}
