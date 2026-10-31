package com.giathinh.uthconnect.presentation.home

import androidx.compose.runtime.Immutable
import com.giathinh.uthconnect.domain.model.Announcement
import com.giathinh.uthconnect.domain.model.AnnouncementCategory
import com.giathinh.uthconnect.domain.model.HomeBanner

/**
 * Trạng thái hiển thị của tab Trang chủ — `data class` bất biến, mọi tham số có giá trị
 * mặc định là trạng thái an toàn nhất lúc màn hình vừa mở (`docs/CODING_STANDARDS.md` mục 3.3).
 *
 * `@Immutable`: Compose bỏ qua recomposition khi tham chiếu không đổi (mục 5).
 *
 * - Không chứa `@StringRes`, `Modifier`, `Context`: ViewModel không cầm tài nguyên UI.
 * - Lỗi là enum, UI map sang chuỗi trong `strings.xml`.
 * - Bộ lọc nhóm là **dữ liệu** trong state (`selectedCategory`), không phải state riêng của
 *   Composable, để danh sách hiển thị suy ra được và test được.
 */
@Immutable
data class HomeUiState(
    val isLoading: Boolean = true,
    val banners: List<HomeBanner> = emptyList(),
    val announcements: List<Announcement> = emptyList(),
    /** `null` = chip "Tất cả". */
    val selectedCategory: AnnouncementCategory? = null,
    val failure: HomeFailure? = null,
) {

    /** Bảng tin đã áp bộ lọc nhóm — Screen chỉ việc render danh sách này. */
    val visibleAnnouncements: List<Announcement>
        get() = selectedCategory
            ?.let { selected -> announcements.filter { it.category == selected } }
            ?: announcements

    /** Rỗng = đã tải xong, không lỗi và bộ lọc hiện tại không còn thông báo nào. */
    val isEmpty: Boolean
        get() = !isLoading && failure == null && visibleAnnouncements.isEmpty()

    /** Số thông báo chưa đọc — nuôi dot trên chuông và badge "Mới". */
    val unreadCount: Int
        get() = announcements.count { it.isUnread }

    /**
     * Số thông báo theo từng nhóm cho dải chip lọc (mục 4.4). Tính một lần lúc dựng state
     * thay vì mỗi lần recomposition: state bất biến nên kết quả không bao giờ cũ.
     */
    val categoryCounts: Map<AnnouncementCategory, Int> =
        announcements.groupingBy { it.category }.eachCount()
}

/** Lý do thất bại, đủ để UI chọn thông điệp và CTA. */
enum class HomeFailure {
    /** Không đọc được bảng tin từ repository. */
    LoadFailed,
}
