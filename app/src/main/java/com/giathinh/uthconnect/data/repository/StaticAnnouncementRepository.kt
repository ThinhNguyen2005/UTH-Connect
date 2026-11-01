package com.giathinh.uthconnect.data.repository

import com.giathinh.uthconnect.domain.model.Announcement
import com.giathinh.uthconnect.domain.model.AnnouncementAttachment
import com.giathinh.uthconnect.domain.model.AnnouncementBlock
import com.giathinh.uthconnect.domain.model.AnnouncementCategory
import com.giathinh.uthconnect.domain.model.AnnouncementDetail
import com.giathinh.uthconnect.domain.model.AttachmentKind
import com.giathinh.uthconnect.domain.model.BannerStyle
import com.giathinh.uthconnect.domain.model.HomeBanner
import com.giathinh.uthconnect.domain.model.HomeFeed
import com.giathinh.uthconnect.domain.model.Milestone
import com.giathinh.uthconnect.domain.repository.AnnouncementRepository
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Bảng tin mẫu để dựng UI trước khi có Firestore (M0 sẽ thay bằng
 * `AnnouncementFirestoreRepository` cùng interface).
 *
 * Nội dung thông báo là **dữ liệu**, không phải chuỗi giao diện: nó sẽ đến từ Firestore và
 * không dịch được theo locale, nên nằm ở đây thay vì `strings.xml`
 * (`docs/CODING_STANDARDS.md` mục 8 chỉ ràng buộc chuỗi khung màn hình).
 *
 * Cố ý để **bốn dạng bài khác nhau** (đầy đủ mốc thời gian + tài liệu, chỉ văn bản, có
 * tài liệu nhưng không có mốc, và bài chỉ có đoạn mở đầu) để màn hình Chi tiết được kiểm
 * tra với nội dung thật sự không đồng nhất.
 *
 * `@Singleton` vì repository không giữ trạng thái; binding interface → impl ở
 * `data/di/HomeDataModule.kt`.
 */
@Singleton
class StaticAnnouncementRepository @Inject constructor() : AnnouncementRepository {

    override suspend fun homeFeed(): Result<HomeFeed> =
        Result.success(HomeFeed(banners = banners, latest = latest))

    override suspend fun detail(id: String): Result<AnnouncementDetail?> =
        Result.success(details[id])

    private val banners = listOf(
        HomeBanner(
            announcement = Announcement(
                id = "banner-hoc-phan",
                category = AnnouncementCategory.TRAINING,
                title = "Thông tin đăng ký học phần học kỳ mới năm học 2026 - 2027",
                publisher = "Phòng Đào tạo",
                publishedAt = LocalDateTime.of(2026, 10, 28, 9, 0),
                source = "Cổng sinh viên",
            ),
            style = BannerStyle.PHOTO,
            documentCode = "108/TB-ĐHGTVT",
            isMandatory = true,
        ),
        HomeBanner(
            announcement = Announcement(
                id = "banner-hoc-bong",
                category = AnnouncementCategory.STUDENT_AFFAIRS,
                title = "Thông báo chương trình hỗ trợ học tập & học bổng Khuyến khích",
                publisher = "Phòng CTSV",
                publishedAt = LocalDateTime.of(2026, 10, 27, 10, 30),
            ),
            style = BannerStyle.PHOTO,
            documentCode = "92/TB-CTSV",
        ),
        HomeBanner(
            announcement = Announcement(
                id = "banner-tinh-nguyen",
                category = AnnouncementCategory.YOUTH_UNION,
                title = "Phát động chiến dịch Tình nguyện Mùa hè xanh & Hội thao UTH 2026",
                summary = "Các liên chi đoàn cơ sở bắt đầu nhận danh sách đăng ký trực tuyến.",
                publisher = "Đoàn Thanh niên UTH",
                publishedAt = LocalDateTime.of(2026, 10, 26, 8, 0),
            ),
            style = BannerStyle.BRAND,
        ),
    )

    private val latest = listOf(
        Announcement(
            id = "tin-dang-ky-hoc-phan",
            category = AnnouncementCategory.TRAINING,
            title = "Kế hoạch đăng ký học phần học kỳ mới & lịch thi bổ sung",
            summary = "Cổng đăng ký mở từ 08:00 ngày 01/11 cho sinh viên khóa 2023 - 2025.",
            publisher = "Phòng Đào tạo",
            publishedAt = LocalDateTime.of(2026, 10, 29, 8, 30),
            source = "Cổng sinh viên",
            isUnread = true,
        ),
        Announcement(
            id = "tin-ho-tro-sinh-vien",
            category = AnnouncementCategory.STUDENT_AFFAIRS,
            title = "Chính sách hỗ trợ sinh viên có hoàn cảnh khó khăn",
            summary = "Hướng dẫn nộp hồ sơ xét trợ cấp xã hội và miễn giảm chi phí học tập đợt 1.",
            publisher = "Phòng CTSV",
            publishedAt = LocalDateTime.of(2026, 10, 27, 14, 0),
        ),
        Announcement(
            id = "tin-hoc-phi",
            category = AnnouncementCategory.TUITION,
            title = "Tra cứu học phí và cổng thanh toán trực tuyến UTH Pay",
            summary = "Hạn chót thanh toán học phí học kỳ I là ngày 15/11.",
            publisher = "Phòng Kế hoạch - Tài chính",
            publishedAt = LocalDateTime.of(2026, 10, 25, 16, 0),
            source = "Trực tuyến",
        ),
        Announcement(
            id = "tin-hoi-thao",
            category = AnnouncementCategory.YOUTH_UNION,
            title = "Hoạt động sắp diễn ra: Hội thao & Ngày hội CLB",
            summary = "Ghi nhận điểm rèn luyện cho sinh viên tham gia cổ vũ và thi đấu các bộ môn.",
            publisher = "Đoàn Trường UTH",
            publishedAt = LocalDateTime.of(2026, 10, 24, 11, 0),
            source = "Cộng điểm ĐRL",
        ),
    )

    /** Dạng đầy đủ: mở đầu, mốc thời gian, gạch đầu dòng, kênh hỗ trợ và hai tệp đính kèm. */
    private val registrationDetail = AnnouncementDetail(
        id = "tin-dang-ky-hoc-phan",
        category = AnnouncementCategory.TRAINING,
        title = "Kế hoạch đăng ký học phần học kỳ mới & lịch thi bổ sung",
        publisher = "Phòng Đào tạo",
        publishedAt = LocalDateTime.of(2026, 10, 28, 9, 0),
        source = "Cổng thông tin Đào tạo UTH",
        updatedAt = LocalDateTime.of(2026, 10, 29, 8, 30),
        lead = "Cổng đăng ký học phần học kỳ mới mở từ 08:00 ngày 01/11/2026 cho toàn bộ sinh " +
            "viên khóa 2023 - 2025. Sinh viên đọc kỹ khung thời gian và chuẩn bị danh sách học " +
            "phần trước khi vào cổng để tránh nghẽn hệ thống.",
        blocks = listOf(
            AnnouncementBlock.Heading("Khung thời gian cụ thể"),
            AnnouncementBlock.Milestones(
                listOf(
                    Milestone(
                        title = "Mở cổng đăng ký",
                        body = "Từ 08:00 ngày 01/11/2026 đến 17:00 ngày 05/11/2026, đăng ký theo " +
                            "đúng khung giờ của từng khóa.",
                        badge = "Đợt 1",
                        emphasized = true,
                    ),
                    Milestone(
                        title = "Điều chỉnh học phần",
                        body = "Từ 08:00 ngày 06/11/2026 đến 17:00 ngày 08/11/2026, chỉ thêm hoặc " +
                            "huỷ học phần còn chỗ.",
                        badge = "Đợt 2",
                    ),
                ),
            ),
            AnnouncementBlock.Heading("Hướng dẫn kỹ thuật"),
            AnnouncementBlock.Bullets(
                listOf(
                    "Đăng nhập cổng sinh viên bằng tài khoản UTH, không dùng email cá nhân.",
                    "Chọn đúng học kỳ và cơ sở trước khi thêm học phần.",
                    "Lưu ảnh phiếu đăng ký sau khi hệ thống xác nhận.",
                ),
            ),
            AnnouncementBlock.Contact(
                label = "Kênh hỗ trợ trực tuyến",
                body = "Trong giờ hành chính, gửi yêu cầu kèm mã số sinh viên.",
                email = "daotao@uth.edu.vn",
            ),
        ),
        attachments = listOf(
            AnnouncementAttachment("Ke-hoach-dang-ky-hoc-phan.pdf", "2.4 MB", AttachmentKind.PDF),
            AnnouncementAttachment("Mau-phieu-dieu-chinh.docx", "340 KB", AttachmentKind.DOCX),
        ),
    )

    /** Dạng chỉ văn bản: không mốc thời gian, không tệp đính kèm, không kênh liên hệ. */
    private val tuitionDetail = AnnouncementDetail(
        id = "tin-hoc-phi",
        category = AnnouncementCategory.TUITION,
        title = "Tra cứu học phí và cổng thanh toán trực tuyến UTH Pay",
        publisher = "Phòng Kế hoạch - Tài chính",
        publishedAt = LocalDateTime.of(2026, 10, 25, 16, 0),
        source = "Trực tuyến",
        lead = "Học phí học kỳ I năm học 2026 - 2027 đã được cập nhật trên cổng UTH Pay. " +
            "Hạn chót thanh toán là ngày 15/11/2026.",
        blocks = listOf(
            AnnouncementBlock.Heading("Số tiền phải nộp"),
            AnnouncementBlock.Paragraph(
                "Số tiền tính theo số tín chỉ đã đăng ký và mức thu của từng nhóm ngành. " +
                    "Sinh viên tra cứu tại mục Học phí trong cổng sinh viên, không dùng số " +
                    "tiền trên biên lai của học kỳ trước.",
            ),
            AnnouncementBlock.Bullets(
                listOf(
                    "Chọn đúng học kỳ và năm học trước khi thanh toán.",
                    "Kiểm tra họ tên và mã số sinh viên ở trang xác nhận.",
                    "Lưu mã giao dịch cho tới khi khoản nộp được ghi nhận.",
                ),
            ),
        ),
    )

    /** Dạng có tệp đính kèm nhưng không có mốc thời gian. */
    private val supportDetail = AnnouncementDetail(
        id = "tin-ho-tro-sinh-vien",
        category = AnnouncementCategory.STUDENT_AFFAIRS,
        title = "Chính sách hỗ trợ sinh viên có hoàn cảnh khó khăn",
        publisher = "Phòng CTSV",
        publishedAt = LocalDateTime.of(2026, 10, 27, 14, 0),
        source = "Cơ sở 1 & 2",
        lead = "Hồ sơ xét trợ cấp xã hội và miễn giảm chi phí học tập đợt 1 nhận tại " +
            "Phòng CTSV tới hết ngày 10/11/2026.",
        blocks = listOf(
            AnnouncementBlock.Heading("Hồ sơ cần chuẩn bị"),
            AnnouncementBlock.Bullets(
                listOf(
                    "Đơn xét trợ cấp có xác nhận của địa phương.",
                    "Bản sao giấy tờ chứng minh hoàn cảnh (hộ nghèo, cận nghèo, khuyết tật…).",
                    "Bảng điểm hoặc giấy xác nhận đang theo học.",
                ),
            ),
            AnnouncementBlock.Paragraph(
                "Sinh viên nộp trực tiếp tại văn phòng hoặc gửi bản scan qua email của phòng. " +
                    "Kết quả xét duyệt được công bố trong vòng 10 ngày làm việc.",
            ),
        ),
        attachments = listOf(
            AnnouncementAttachment("Mau-don-xet-tro-cap.docx", "180 KB", AttachmentKind.DOCX),
        ),
    )

    /** Dạng có mốc thời gian và tệp đính kèm nhưng không có khối liên hệ. */
    private val scholarshipDetail = AnnouncementDetail(
        id = "banner-hoc-bong",
        category = AnnouncementCategory.STUDENT_AFFAIRS,
        title = "Thông báo chương trình hỗ trợ học tập & học bổng Khuyến khích",
        publisher = "Phòng CTSV",
        publishedAt = LocalDateTime.of(2026, 10, 27, 10, 30),
        source = "Phòng Công tác sinh viên",
        lead = "Học bổng Khuyến khích học kỳ I xét cho sinh viên có điểm trung bình từ 3.0 và " +
            "không có môn nợ. Hồ sơ nộp trực tuyến tới hết ngày 12/11/2026.",
        blocks = listOf(
            AnnouncementBlock.Heading("Mốc thời gian"),
            AnnouncementBlock.Milestones(
                listOf(
                    Milestone(
                        title = "Nộp hồ sơ",
                        body = "Tới 17:00 ngày 12/11/2026 trên cổng sinh viên.",
                        badge = "Đợt 1",
                        emphasized = true,
                    ),
                    Milestone(
                        title = "Công bố kết quả",
                        body = "Ngày 20/11/2026, danh sách gửi qua email trường.",
                    ),
                ),
            ),
            AnnouncementBlock.Bullets(
                listOf(
                    "Điểm trung bình tích luỹ từ 3.0, không có môn nợ.",
                    "Không vi phạm quy chế thi trong học kỳ xét.",
                ),
            ),
        ),
        attachments = listOf(
            AnnouncementAttachment("Quy-che-hoc-bong-khuyen-khich.pdf", "1.1 MB", AttachmentKind.PDF),
        ),
    )

    /** Dạng phong trào: đoạn mở đầu + việc cần làm + một đoạn văn, không mốc thời gian. */
    private val youthUnionDetail = AnnouncementDetail(
        id = "tin-hoi-thao",
        category = AnnouncementCategory.YOUTH_UNION,
        title = "Hoạt động sắp diễn ra: Hội thao & Ngày hội CLB",
        publisher = "Đoàn Trường UTH",
        publishedAt = LocalDateTime.of(2026, 10, 24, 11, 0),
        source = "Cộng điểm ĐRL",
        lead = "Hội thao và Ngày hội CLB diễn ra trong hai ngày 07 - 08/11/2026 tại sân vận " +
            "động cơ sở 1. Sinh viên tham gia cổ thi đấu hay cổ vũ đều được ghi nhận điểm rèn luyện.",
        blocks = listOf(
            AnnouncementBlock.Heading("Việc cần làm trước ngày thi đấu"),
            AnnouncementBlock.Bullets(
                listOf(
                    "Đăng ký môn thi với liên chi đoàn trước ngày 03/11.",
                    "Có mặt trước giờ thi 30 phút để nhận số báo danh.",
                    "Mang thẻ sinh viên khi vào sân.",
                ),
            ),
            AnnouncementBlock.Paragraph(
                "Danh sách đội hình và lịch thi đấu được cập nhật tại bảng tin của Đoàn trường.",
            ),
        ),
    )

    /**
     * Bài nào chưa có nội dung riêng thì lấy đoạn tóm tắt của bảng tin làm đoạn mở đầu —
     * màn hình Chi tiết không bao giờ rỗng, và layout cũng được kiểm tra với bài ngắn.
     */
    private val details: Map<String, AnnouncementDetail> = buildMap {
        (banners.map { it.announcement } + latest).forEach { announcement ->
            put(announcement.id, announcement.brief())
        }
        listOf(
            registrationDetail,
            registrationDetail.copy(id = "banner-hoc-phan"),
            scholarshipDetail,
            tuitionDetail,
            supportDetail,
            youthUnionDetail,
            youthUnionDetail.copy(
                id = "banner-tinh-nguyen",
                title = "Phát động chiến dịch Tình nguyện Mùa hè xanh & Hội thao UTH 2026",
                publisher = "Đoàn Thanh niên UTH",
                publishedAt = LocalDateTime.of(2026, 10, 26, 8, 0),
            ),
        ).forEach { put(it.id, it) }
    }
}

/** Bài ngắn: chỉ tiêu đề + đoạn mở đầu, không khối nội dung, không tệp đính kèm. */
private fun Announcement.brief(): AnnouncementDetail = AnnouncementDetail(
    id = id,
    category = category,
    title = title,
    publisher = publisher,
    publishedAt = publishedAt,
    source = source,
    lead = summary,
)
