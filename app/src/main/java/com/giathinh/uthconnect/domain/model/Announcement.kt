package com.giathinh.uthconnect.domain.model

import java.time.LocalDateTime

/**
 * Nhóm thông báo của nhà trường, dùng cho badge trên thẻ và chip lọc ở tab Trang chủ.
 *
 * Là `enum` chứ không phải `String` (`docs/CODING_STANDARDS.md` mục 3.3): nhãn hiển thị
 * nằm trong `strings.xml`, còn màu badge do UI quyết định qua `BadgeTone`.
 */
enum class AnnouncementCategory {
    /** Phòng Đào tạo: đăng ký học phần, lịch thi. */
    TRAINING,

    /** Phòng Công tác sinh viên: học bổng, trợ cấp, chính sách. */
    STUDENT_AFFAIRS,

    /** Học phí và các khoản phải nộp. */
    TUITION,

    /** Đoàn – Hội: phong trào, hội thao, tình nguyện. */
    YOUTH_UNION,
}

/**
 * Một thông báo hiển thị trên bảng tin trang chủ.
 *
 * `title`, `summary`, `publisher` là **dữ liệu** (nguồn thật sẽ là Firestore), không phải
 * chuỗi giao diện — nên chúng không nằm trong `strings.xml`. Chuỗi khung màn hình
 * ("Bảng tin", "Mới", "Tất cả"…) vẫn bắt buộc đi qua `stringResource`.
 *
 * Không chứa `@StringRes`, `Color` hay `Modifier`: domain không biết gì về Android.
 */
data class Announcement(
    val id: String,
    val category: AnnouncementCategory,
    val title: String,
    /** Tóm tắt một dòng cho danh sách; `null` khi thông báo chỉ có tiêu đề. */
    val summary: String? = null,
    /** Đơn vị phát hành, hiển thị cạnh thời gian đăng. */
    val publisher: String,
    val publishedAt: LocalDateTime,
    /**
     * Nơi đăng hoặc ghi chú nguồn ("Cổng sinh viên", "Cơ sở 1 & 2"). `null` thì UI hiện
     * ngày đăng — cùng một dòng meta, không thêm dòng thứ hai.
     */
    val source: String? = null,
    /** Chưa đọc — UI đánh dấu bằng badge "Mới", không chỉ bằng màu. */
    val isUnread: Boolean = false,
)
