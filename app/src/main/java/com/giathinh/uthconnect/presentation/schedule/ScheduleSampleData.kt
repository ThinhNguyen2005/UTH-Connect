package com.giathinh.uthconnect.presentation.schedule

import java.time.LocalDate

// Một bản ghi là một buổi học, không phải toàn bộ học phần.
data class CourseClassItem(
    val id: String,
    val courseName: String, // Tên môn học
    val classCode: String, // Mã lớp học phần
    val credits: Int, // Số tín chỉ
    val startPeriod: Int, // Tiết bắt đầu
    val endPeriod: Int, // Tiết kết thúc
    val timeRange: String,
    val room: String,
    val campus: String, // Cơ sở học hoặc thi
    val lecturer: String,
    val date: LocalDate,
)

data class ExamItem(
    val id: String,
    val course: CourseClassItem,
    val date: LocalDate,
    val session: String,
    val room: String,
    val campus: String, // Cơ sở học hoặc thi
    val method: String,
)

// Lịch mẫu cố định trong tuần 26/10–01/11/2026, chưa lấy từ cổng đào tạo.
// plusDays(1) là thứ Ba, plusDays(2) là thứ Tư, ...
private val monday = LocalDate.of(2026, 10, 26)
val sampleClasses = listOf(
    CourseClassItem(
        id = "1",
        courseName = "Lập trình thiết bị di động",
        classCode = "010100085402",
        credits = 3,
        startPeriod = 1,
        endPeriod = 3,
        timeRange = "07:00 – 09:15",
        room = "B102",
        campus = "CS1 Võ Oanh",
        lecturer = "Nguyễn Văn An",
        date = monday,
    ),
    CourseClassItem(
        id = "2",
        courseName = "Cơ sở dữ liệu",
        classCode = "010100072301",
        credits = 3,
        startPeriod = 7,
        endPeriod = 9,
        timeRange = "12:30 – 14:45",
        room = "C203",
        campus = "CS1 Võ Oanh",
        lecturer = "Trần Thị Bình",
        date = monday,
    ),
    CourseClassItem(
        id = "3",
        courseName = "Mạng máy tính",
        classCode = "010100063201",
        credits = 3,
        startPeriod = 1,
        endPeriod = 3,
        timeRange = "07:00 – 09:15",
        room = "A302",
        campus = "CS2 Bình Triệu",
        lecturer = "Lê Minh Tuấn",
        date = monday.plusDays(1),
    ),
    CourseClassItem(
        id = "4",
        courseName = "Công nghệ phần mềm",
        classCode = "010100091101",
        credits = 3,
        startPeriod = 4,
        endPeriod = 6,
        timeRange = "09:30 – 11:45",
        room = "B204",
        campus = "CS1 Võ Oanh",
        lecturer = "Phạm Thu Hà",
        date = monday.plusDays(2),
    ),
    CourseClassItem(
        id = "5",
        courseName = "Tiếng Anh chuyên ngành công nghệ thông tin",
        classCode = "010100032101",
        credits = 2,
        startPeriod = 7,
        endPeriod = 9,
        timeRange = "12:30 – 14:45",
        room = "D105",
        campus = "Phân hiệu Đồng Nai",
        lecturer = "Võ Ngọc Mai",
        date = monday.plusDays(3),
    ),
    CourseClassItem(
        id = "6",
        courseName = "Lập trình thiết bị di động",
        classCode = "010100085402",
        credits = 3,
        startPeriod = 1,
        endPeriod = 3,
        timeRange = "07:00 – 09:15",
        room = "PM02",
        campus = "CS1 Võ Oanh",
        lecturer = "Nguyễn Văn An",
        date = monday.plusDays(4),
    ),
    CourseClassItem(
        id = "7",
        courseName = "Giáo dục thể chất",
        classCode = "010100011201",
        credits = 1,
        startPeriod = 1,
        endPeriod = 3,
        timeRange = "07:00 – 09:15",
        room = "Sân thể thao",
        campus = "CS2 Bình Triệu",
        lecturer = "Đặng Thành Nam",
        date = monday.plusDays(5),
    ),
        )

// Viết từng lịch thi ra rõ ràng để dễ sửa ngày, ca thi, phòng và hình thức thi.
// sampleClasses[0] là môn đầu tiên trong danh sách lịch học bên trên.
val sampleExams = listOf(
    ExamItem(
        id = "exam-1",
        course = sampleClasses[0],
        date = LocalDate.of(2026, 11, 16),
        session = "Ca 1 • 07:00 • 90 phút",
        room = "B201",
        campus = "CS1 Võ Oanh",
        method = "Thực hành",
    ),
    ExamItem(
        id = "exam-2",
        course = sampleClasses[1],
        date = LocalDate.of(2026, 11, 17),
        session = "Ca 1 • 07:00 • 90 phút",
        room = "B202",
        campus = "CS1 Võ Oanh",
        method = "Tự luận",
    ),
    ExamItem(
        id = "exam-3",
        course = sampleClasses[2],
        date = LocalDate.of(2026, 11, 18),
        session = "Ca 1 • 07:00 • 90 phút",
        room = "B203",
        campus = "CS1 Võ Oanh",
        method = "Tự luận",
    ),
)
