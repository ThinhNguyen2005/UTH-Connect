package com.giathinh.uthconnect.data.repository

import com.giathinh.uthconnect.domain.model.TuitionCourse
import com.giathinh.uthconnect.domain.model.TuitionInfo
import com.giathinh.uthconnect.domain.repository.TuitionRepository
import javax.inject.Inject
import javax.inject.Singleton

private const val SAMPLE_COURSE_CREDITS = 3

@Singleton
class StaticTuitionRepository @Inject constructor() : TuitionRepository {

    override suspend fun getTuition(): Result<TuitionInfo> = Result.success(
        TuitionInfo(
            semester = "Học kỳ 1 (Năm học 2025 - 2026)",
            isPaid = true,
            totalAmount = "12.650.000 VNĐ",
            paidAmount = "12.650.000 VNĐ",
            remainingAmount = "0 VNĐ",
            creditsCount = 18,
            dueDate = "15/11/2025",
            courses = listOf(
                TuitionCourse("IT001", "Lập trình thiết bị di động", SAMPLE_COURSE_CREDITS, "2.100.000 VNĐ"),
                TuitionCourse("IT002", "Kiến trúc & Thiết kế phần mềm", SAMPLE_COURSE_CREDITS, "2.100.000 VNĐ"),
                TuitionCourse("IT003", "Kiểm thử & Đảm bảo chất lượng", SAMPLE_COURSE_CREDITS, "2.100.000 VNĐ"),
                TuitionCourse("IT004", "Công nghệ phần mềm chuyên sâu", SAMPLE_COURSE_CREDITS, "2.100.000 VNĐ"),
                TuitionCourse("IT005", "Đồ án chuyên ngành KTPM", SAMPLE_COURSE_CREDITS, "2.100.000 VNĐ"),
                TuitionCourse("GE006", "Kỹ năng nghề nghiệp & Giao tiếp", SAMPLE_COURSE_CREDITS, "2.150.000 VNĐ"),
            ),
        ),
    )
}
