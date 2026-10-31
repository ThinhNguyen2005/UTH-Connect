package com.giathinh.uthconnect.domain.model

/**
 * Data Model Thời khóa biểu & Lịch học sinh viên UTH.
 * Phục vụ module KAN-31 (Văn Tạ Huy Thiên).
 */
data class CourseSchedule(
    val id: String,
    val courseCode: String,
    val courseName: String,
    val lecturerName: String,
    val room: String,
    val campus: String, // "Quang Trung" | "Nguyễn Tất Thành" | "Thủ Đức"
    val dayOfWeek: Int, // 2 = Thứ Hai, 3 = Thứ Ba, ..., 8 = Chủ Nhật
    val startPeriod: Int, // Tiết bắt đầu (1, 4, 7, 10...)
    val endPeriod: Int,   // Tiết kết thúc (3, 6, 9, 12...)
    val startTime: String, // "07:00"
    val endTime: String,   // "09:30"
    val date: String,     // "YYYY-MM-DD" nếu là lịch của một ngày cụ thể
    val credits: Int = 3,
    val note: String? = null,
)
