package com.giathinh.uthconnect.domain.model

/**
 * Data Model Bảng điểm & Kết quả học tập sinh viên UTH.
 * Phục vụ module Tiện ích & Cài đặt (Thắng Nguyễn Minh KAN-30) và Profile (Anh Phú KAN-32).
 */
data class StudentGrade(
    val id: String,
    val courseCode: String,
    val courseName: String,
    val credits: Int,
    val semester: String, // ví dụ "HK1 2025-2026", "HK2 2025-2026"
    val attendanceScore: Float?, // Điểm chuyên cần (10%)
    val midtermScore: Float?,    // Điểm giữa kỳ (30%)
    val finalScore: Float?,      // Điểm cuối kỳ (60%)
    val totalScore10: Float,     // Điểm tổng kết hệ 10
    val totalScore4: Float,      // Điểm tổng kết hệ 4
    val letterGrade: String,     // Điểm chữ: A, B+, B, C+, C, D+, D, F
)

data class SemesterGpa(
    val semester: String,
    val gpa10: Float,
    val gpa4: Float,
    val totalCredits: Int,
)
