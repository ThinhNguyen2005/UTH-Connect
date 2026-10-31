package com.giathinh.uthconnect.presentation.utilities.tuition

/**
 * Một học phần đăng ký trong kỳ học.
 */
data class TuitionCourse(
    val code: String,
    val name: String,
    val credits: Int,
    val fee: String,
)

/**
 * Trạng thái giao diện màn hình Tra cứu học phí.
 */
data class TuitionUiState(
    val semester: String = "Học kỳ 1 (Năm học 2025 - 2026)",
    val isPaid: Boolean = true,
    val totalAmount: String = "12.650.000 VNĐ",
    val paidAmount: String = "12.650.000 VNĐ",
    val remainingAmount: String = "0 VNĐ",
    val creditsCount: Int = 18,
    val dueDate: String = "15/11/2025",
    val courses: List<TuitionCourse> = listOf(
        TuitionCourse("IT001", "Lập trình thiết bị di động", 3, "2.100.000 VNĐ"),
        TuitionCourse("IT002", "Kiến trúc & Thiết kế phần mềm", 3, "2.100.000 VNĐ"),
        TuitionCourse("IT003", "Kiểm thử & Đảm bảo chất lượng", 3, "2.100.000 VNĐ"),
        TuitionCourse("IT004", "Công nghệ phần mềm chuyên sâu", 3, "2.100.000 VNĐ"),
        TuitionCourse("IT005", "Đồ án chuyên ngành KTPM", 3, "2.100.000 VNĐ"),
        TuitionCourse("GE006", "Kỹ năng nghề nghiệp & Giao tiếp", 3, "2.150.000 VNĐ"),
    ),
)
