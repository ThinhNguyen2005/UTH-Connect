package com.giathinh.uthconnect.domain.model

/**
 * Kiểu trình bày của banner trong carousel trang chủ.
 *
 * Tách khỏi [Announcement] vì đây là quyết định trình bày của nguồn nội dung, không phải
 * thuộc tính của thông báo: cùng một thông báo có thể được ghim lên đầu bảng tin hoặc
 * chỉ nằm trong danh sách.
 */
enum class BannerStyle {
    /** Banner có ảnh bìa 16:9; chưa có ảnh thì UI hiện `SurfaceSubtle` + icon nhóm. */
    PHOTO,

    /**
     * Banner nền Navy (gradient thương hiệu) cho bản tin nhanh — trạng thái dự phòng khi
     * thông báo không có ảnh bìa nhưng vẫn cần nổi bật.
     */
    BRAND,
}

/** Một thẻ trong carousel đầu trang chủ. */
data class HomeBanner(
    val announcement: Announcement,
    val style: BannerStyle = BannerStyle.PHOTO,
    /** Số/ký hiệu văn bản ("108/TB-ĐHGTVT"); `null` thì UI không vẽ chip mã văn bản. */
    val documentCode: String? = null,
    /**
     * Văn bản bắt buộc sinh viên phải đọc. UI nhấn bằng chữ "Bắt buộc", không đổi màu
     * tiêu đề — trạng thái không bao giờ chỉ truyền bằng màu.
     */
    val isMandatory: Boolean = false,
) {
    val id: String
        get() = announcement.id
}

/**
 * Dữ liệu một lần nạp của tab Trang chủ.
 *
 * Gói cả banner và danh sách vào một kết quả để màn hình chỉ có **một** trạng thái tải:
 * hai lời gọi riêng sẽ sinh hai đường lỗi và hai spinner lệch nhau (`docs/CODING_STANDARDS.md`
 * mục 6 — một nguồn sự thật mỗi màn hình).
 */
data class HomeFeed(
    val banners: List<HomeBanner> = emptyList(),
    val latest: List<Announcement> = emptyList(),
)
