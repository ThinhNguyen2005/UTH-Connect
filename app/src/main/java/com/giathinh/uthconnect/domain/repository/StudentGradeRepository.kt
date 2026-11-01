package com.giathinh.uthconnect.domain.repository

import com.giathinh.uthconnect.domain.model.SemesterGpa
import com.giathinh.uthconnect.domain.model.StudentGrade
import kotlinx.coroutines.flow.Flow

/**
 * Interface Repository quản lý bảng điểm & kết quả học tập sinh viên.
 * Phục vụ KAN-30 (Thắng – Tiện ích & Cài đặt) và KAN-32 (Anh Phú – Profile).
 */
interface StudentGradeRepository {
    /** Lấy danh sách điểm theo [semester] (ví dụ: "HK1 2025-2026"). Truyền null để lấy tất cả. */
    fun getGrades(semester: String? = null): Flow<List<StudentGrade>>

    /** Lấy GPA từng học kỳ để hiển thị biểu đồ tiến trình học tập. */
    fun getSemesterGpaList(): Flow<List<SemesterGpa>>

    /** GPA tích luỹ tổng toàn khoá (hệ 4). */
    fun getCumulativeGpa(): Flow<Float>
}
