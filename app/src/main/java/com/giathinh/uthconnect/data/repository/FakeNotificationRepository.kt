package com.giathinh.uthconnect.data.repository

import com.giathinh.uthconnect.domain.model.NotificationCategory
import com.giathinh.uthconnect.domain.model.PostNotification
import com.giathinh.uthconnect.domain.repository.NotificationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

/**
 * Fake/Static implementation của [NotificationRepository] dùng để UI team
 * (KAN-28 – Đức Địch Việt) có thể lấy dữ liệu ngay mà không cần API thật.
 *
 * Thay thế bằng [FirestoreNotificationRepository] khi backend sẵn sàng.
 */
class FakeNotificationRepository @Inject constructor() : NotificationRepository {

    // -----------------------------------------------------------------------
    // Sample data – Thông báo mẫu
    // -----------------------------------------------------------------------
    private val sampleNotifications: List<PostNotification> = listOf(
        PostNotification(
            id = "notif-001",
            title = "Lịch thi cuối kỳ HK1 năm học 2025-2026",
            summary = "Phòng Đào tạo thông báo lịch thi cuối kỳ cho toàn thể sinh viên.",
            content = """
                Phòng Đào tạo thông báo lịch thi cuối kỳ HK1 năm học 2025-2026 như sau:
                
                - Thời gian thi: Từ ngày 06/01/2026 đến 24/01/2026
                - Địa điểm thi: Các phòng thi theo kế hoạch (xem bảng đính kèm)
                - Sinh viên phải mang theo thẻ sinh viên khi thi
                
                Mọi thắc mắc liên hệ Phòng Đào tạo – phòng A102.
            """.trimIndent(),
            category = NotificationCategory.EXAMINATION,
            author = "Phòng Đào tạo",
            publishedAt = 1735689600000L, // 2026-01-01
            isPinned = true,
        ),
        PostNotification(
            id = "notif-002",
            title = "Thông báo đóng học phí HK1 2025-2026 đợt 2",
            summary = "Hạn cuối đóng học phí đợt 2 là ngày 15/11/2025. Sinh viên chưa đóng sẽ bị khoá tài khoản.",
            content = """
                Phòng Công tác Sinh viên thông báo:
                
                Sinh viên chưa đóng học phí HK1 2025-2026 vui lòng hoàn thành trước ngày 15/11/2025.
                Sau thời hạn trên, tài khoản hệ thống sẽ bị tạm khoá và không thể đăng ký học phần HK2.
                
                Hình thức đóng:
                1. Chuyển khoản ngân hàng (xem thông tin tài khoản trên cổng sinh viên)
                2. Đóng trực tiếp tại phòng Tài vụ – A103
                
                Liên hệ: phòng Tài vụ – 028.3456.7890
            """.trimIndent(),
            category = NotificationCategory.TUITION,
            author = "Phòng Tài vụ",
            publishedAt = 1730592000000L, // 2025-11-03
            isPinned = true,
        ),
        PostNotification(
            id = "notif-003",
            title = "Đăng ký học phần HK2 năm học 2025-2026",
            summary = "Sinh viên đăng ký học phần HK2 từ ngày 20/11 đến 05/12/2025 trên cổng sinh viên.",
            content = """
                Phòng Đào tạo thông báo kế hoạch đăng ký học phần HK2 2025-2026:
                
                - Đợt 1 (ưu tiên): 20/11 – 25/11/2025 (sinh viên từ năm 3 trở lên)
                - Đợt 2: 26/11 – 05/12/2025 (tất cả sinh viên)
                - Điều chỉnh: 08/12 – 12/12/2025
                
                Lưu ý: Phải hoàn thành đóng học phí trước khi đăng ký.
            """.trimIndent(),
            category = NotificationCategory.ACADEMIC,
            author = "Phòng Đào tạo",
            publishedAt = 1731110400000L, // 2025-11-09
        ),
        PostNotification(
            id = "notif-004",
            title = "Hội thao Sinh viên UTH 2025 – Kết quả & Trao giải",
            summary = "Lễ trao giải Hội thao Sinh viên UTH 2025 sẽ diễn ra vào ngày 15/11/2025 tại sân thể thao.",
            content = """
                Đoàn – Hội UTH trân trọng thông báo kết quả Hội thao Sinh viên UTH 2025:
                
                🏆 Khoa Công nghệ Thông tin đoạt giải Nhất toàn đoàn
                🥈 Khoa Kinh tế – Quản trị kinh doanh – Nhì toàn đoàn
                🥉 Khoa Cơ khí – Ba toàn đoàn
                
                Lễ trao giải và bế mạc: 15/11/2025, 14h00, Sân thể thao chính.
                Mời toàn thể sinh viên đến tham dự và cổ vũ.
            """.trimIndent(),
            category = NotificationCategory.ACTIVITIES,
            author = "Đoàn - Hội UTH",
            publishedAt = 1731024000000L, // 2025-11-08
        ),
        PostNotification(
            id = "notif-005",
            title = "Lịch nghỉ lễ Quốc khánh & kế hoạch học bù",
            summary = "Trường UTH thông báo lịch nghỉ lễ 02/09/2025 và kế hoạch học bù.",
            content = """
                Ban Giám hiệu trường UTH thông báo lịch nghỉ lễ Quốc khánh 02/09/2025:
                
                - Nghỉ: Thứ 3 ngày 02/09/2025
                - Học bù: Thứ 7 ngày 06/09/2025 (áp dụng cho các lớp học ngày Thứ 3)
                
                Lịch học bù chi tiết sẽ được đăng tải trên cổng sinh viên.
            """.trimIndent(),
            category = NotificationCategory.GENERAL,
            author = "Ban Giám hiệu",
            publishedAt = 1725004800000L, // 2025-08-30
        ),
        PostNotification(
            id = "notif-006",
            title = "Workshop Định hướng Nghề nghiệp – Ngành CNTT",
            summary = "Khoa CNTT tổ chức workshop 'Nghề nghiệp CNTT 2025' với sự tham gia của doanh nghiệp FPT, Viettel.",
            content = """
                Khoa Công nghệ Thông tin phối hợp với doanh nghiệp tổ chức Workshop:
                "Định hướng Nghề nghiệp ngành CNTT 2025"
                
                📅 Thời gian: 14h00 – 17h00, Thứ 6 ngày 14/11/2025
                📍 Địa điểm: Hội trường A – Cơ sở Quang Trung
                🎤 Diễn giả: Đại diện FPT Software, Viettel Digital, TMA Solutions
                
                Nội dung:
                - Xu hướng tuyển dụng CNTT 2025-2026
                - Kỹ năng cần thiết cho Junior Developer
                - Q&A trực tiếp với HR
                
                Đăng ký miễn phí tại: uth.edu.vn/workshop-cntt-2025
            """.trimIndent(),
            category = NotificationCategory.ACTIVITIES,
            author = "Khoa Công nghệ Thông tin",
            publishedAt = 1730851200000L, // 2025-11-06
        ),
        PostNotification(
            id = "notif-007",
            title = "Cập nhật quy chế học vụ năm 2025",
            summary = "Nhà trường ban hành quy chế học vụ mới áp dụng từ HK1 2025-2026. Sinh viên cần đọc kỹ.",
            content = """
                Ban Giám hiệu thông báo về việc cập nhật Quy chế Học vụ năm 2025:
                
                Những thay đổi chính:
                1. Điều kiện xét tốt nghiệp: GPA ≥ 2.0/4.0 (trước là 1.8)
                2. Số tín chỉ tối đa mỗi học kỳ: 24 TC (trước là 26 TC)
                3. Quy định về điểm rèn luyện: Tối thiểu 50/100 điểm
                4. Chính sách học lại: Tối đa 2 lần/môn học
                
                Quy chế đầy đủ: uth.edu.vn/quy-che-hoc-vu-2025
            """.trimIndent(),
            category = NotificationCategory.ACADEMIC,
            author = "Phòng Đào tạo",
            publishedAt = 1728518400000L, // 2025-10-10
        ),
        PostNotification(
            id = "notif-008",
            title = "Học bổng Vingroup 2025 – Hạn nộp hồ sơ 30/11",
            summary = "Vingroup dành 50 suất học bổng cho sinh viên xuất sắc ngành CNTT, KT, Cơ khí.",
            content = """
                Phòng Công tác Sinh viên thông báo chương trình Học bổng Vingroup 2025:
                
                🎓 Số lượng: 50 suất (trị giá 20 triệu đồng/suất)
                📋 Đối tượng: Sinh viên năm 2, 3 ngành CNTT – Kinh tế – Cơ khí
                📊 Điều kiện: GPA ≥ 3.2/4.0, không có môn học lại
                📅 Hạn nộp: 30/11/2025
                
                Hồ sơ nộp tại Phòng Công tác Sinh viên – A104 hoặc qua email: hocbong@uth.edu.vn
                
                Chi tiết: uth.edu.vn/hoc-bong-vingroup-2025
            """.trimIndent(),
            category = NotificationCategory.TUITION,
            author = "Phòng Công tác Sinh viên",
            publishedAt = 1731369600000L, // 2025-11-12
        ),
    )

    // -----------------------------------------------------------------------
    // Interface implementation
    // -----------------------------------------------------------------------

    /** Trả về newsfeed, lọc theo [category] (ALL = không lọc, pinned lên đầu). */
    override fun getNewsfeed(category: NotificationCategory): Flow<List<PostNotification>> {
        val filtered = if (category == NotificationCategory.ALL) {
            sampleNotifications
        } else {
            sampleNotifications.filter { it.category == category }
        }
        // Pinned items lên đầu, sau đó sắp xếp theo thời gian mới nhất
        val sorted = filtered.sortedWith(
            compareByDescending<PostNotification> { it.isPinned }.thenByDescending { it.publishedAt }
        )
        return flowOf(sorted)
    }

    /** Trả về chi tiết một thông báo theo [notificationId]. */
    override suspend fun getNotificationDetail(notificationId: String): Result<PostNotification?> =
        runCatching {
            sampleNotifications.firstOrNull { it.id == notificationId }
        }
}
