package com.giathinh.uthconnect.data.repository

import com.giathinh.uthconnect.domain.model.SemesterGpa
import com.giathinh.uthconnect.domain.model.StudentGrade
import com.giathinh.uthconnect.domain.repository.StudentGradeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

/**
 * Fake/Static implementation của [StudentGradeRepository] dùng để UI team
 * (KAN-30 – Thắng, KAN-32 – Anh Phú) lấy dữ liệu ngay không cần API.
 *
 * Thay thế bằng [FirestoreGradeRepository] khi backend sẵn sàng.
 */
class FakeStudentGradeRepository @Inject constructor() : StudentGradeRepository {

    // -----------------------------------------------------------------------
    // Sample data – Bảng điểm mẫu
    // -----------------------------------------------------------------------
    private val sampleGrades: List<StudentGrade> = listOf(

        // ── HK1 2024-2025 ──────────────────────────────────────────────────
        StudentGrade(
            id = "grade-001",
            courseCode = "MTH101",
            courseName = "Toán cao cấp A1",
            credits = 3,
            semester = "HK1 2024-2025",
            attendanceScore = 9.0f,
            midtermScore = 7.5f,
            finalScore = 8.0f,
            totalScore10 = 8.0f,
            totalScore4 = 3.5f,
            letterGrade = "B+",
        ),
        StudentGrade(
            id = "grade-002",
            courseCode = "PHY101",
            courseName = "Vật lý đại cương",
            credits = 3,
            semester = "HK1 2024-2025",
            attendanceScore = 8.5f,
            midtermScore = 7.0f,
            finalScore = 7.5f,
            totalScore10 = 7.5f,
            totalScore4 = 3.0f,
            letterGrade = "B",
        ),
        StudentGrade(
            id = "grade-003",
            courseCode = "ENG101",
            courseName = "Tiếng Anh 1",
            credits = 3,
            semester = "HK1 2024-2025",
            attendanceScore = 10.0f,
            midtermScore = 9.0f,
            finalScore = 9.5f,
            totalScore10 = 9.5f,
            totalScore4 = 4.0f,
            letterGrade = "A",
        ),
        StudentGrade(
            id = "grade-004",
            courseCode = "CS101",
            courseName = "Nhập môn Lập trình",
            credits = 4,
            semester = "HK1 2024-2025",
            attendanceScore = 9.5f,
            midtermScore = 8.5f,
            finalScore = 9.0f,
            totalScore10 = 9.0f,
            totalScore4 = 4.0f,
            letterGrade = "A",
        ),

        // ── HK2 2024-2025 ──────────────────────────────────────────────────
        StudentGrade(
            id = "grade-005",
            courseCode = "CS201",
            courseName = "Lập trình Hướng đối tượng",
            credits = 4,
            semester = "HK2 2024-2025",
            attendanceScore = 9.0f,
            midtermScore = 8.0f,
            finalScore = 8.5f,
            totalScore10 = 8.5f,
            totalScore4 = 3.5f,
            letterGrade = "B+",
        ),
        StudentGrade(
            id = "grade-006",
            courseCode = "MTH201",
            courseName = "Toán Rời Rạc",
            credits = 3,
            semester = "HK2 2024-2025",
            attendanceScore = 8.0f,
            midtermScore = 7.5f,
            finalScore = 8.0f,
            totalScore10 = 7.9f,
            totalScore4 = 3.0f,
            letterGrade = "B",
        ),
        StudentGrade(
            id = "grade-007",
            courseCode = "ENG201",
            courseName = "Tiếng Anh 2",
            credits = 3,
            semester = "HK2 2024-2025",
            attendanceScore = 9.5f,
            midtermScore = 8.5f,
            finalScore = 9.0f,
            totalScore10 = 9.0f,
            totalScore4 = 4.0f,
            letterGrade = "A",
        ),
        StudentGrade(
            id = "grade-008",
            courseCode = "DSA201",
            courseName = "Cấu trúc Dữ liệu & Giải thuật",
            credits = 4,
            semester = "HK2 2024-2025",
            attendanceScore = 9.0f,
            midtermScore = 8.0f,
            finalScore = 8.5f,
            totalScore10 = 8.5f,
            totalScore4 = 3.5f,
            letterGrade = "B+",
        ),

        // ── HK1 2025-2026 (học kỳ hiện tại – đang học) ───────────────────
        StudentGrade(
            id = "grade-009",
            courseCode = "AND301",
            courseName = "Lập trình Android",
            credits = 3,
            semester = "HK1 2025-2026",
            attendanceScore = 10.0f,
            midtermScore = null, // Chưa có điểm giữa kỳ
            finalScore = null,
            totalScore10 = 0f,
            totalScore4 = 0f,
            letterGrade = "—",
        ),
        StudentGrade(
            id = "grade-010",
            courseCode = "DB301",
            courseName = "Cơ sở Dữ liệu Nâng cao",
            credits = 4,
            semester = "HK1 2025-2026",
            attendanceScore = 9.5f,
            midtermScore = null,
            finalScore = null,
            totalScore10 = 0f,
            totalScore4 = 0f,
            letterGrade = "—",
        ),
    )

    private val semesterGpaList: List<SemesterGpa> = listOf(
        SemesterGpa(semester = "HK1 2024-2025", gpa10 = 8.5f, gpa4 = 3.6f, totalCredits = 13),
        SemesterGpa(semester = "HK2 2024-2025", gpa10 = 8.5f, gpa4 = 3.5f, totalCredits = 14),
        // HK1 2025-2026: chưa có điểm, không thêm vào
    )

    // -----------------------------------------------------------------------
    // Interface implementation
    // -----------------------------------------------------------------------

    override fun getGrades(semester: String?): Flow<List<StudentGrade>> {
        val filtered = if (semester == null) sampleGrades
        else sampleGrades.filter { it.semester == semester }
        return flowOf(filtered)
    }

    override fun getSemesterGpaList(): Flow<List<SemesterGpa>> =
        flowOf(semesterGpaList)

    override fun getCumulativeGpa(): Flow<Float> {
        // Tính GPA tích luỹ (weighted average theo số tín chỉ)
        val completedGrades = sampleGrades.filter { it.totalScore4 > 0f }
        val totalWeighted = completedGrades.sumOf { (it.totalScore4 * it.credits).toDouble() }
        val totalCredits = completedGrades.sumOf { it.credits }
        val cumulativeGpa = if (totalCredits > 0) (totalWeighted / totalCredits).toFloat() else 0f
        return flowOf(cumulativeGpa)
    }
}
