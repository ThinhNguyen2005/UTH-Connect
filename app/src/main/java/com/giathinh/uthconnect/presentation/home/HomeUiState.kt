package com.giathinh.uthconnect.presentation.home

import androidx.compose.runtime.Immutable
import com.giathinh.uthconnect.domain.model.Announcement
import com.giathinh.uthconnect.domain.model.AnnouncementCategory
import com.giathinh.uthconnect.domain.model.BookListing
import com.giathinh.uthconnect.domain.model.BookListingStatus
import com.giathinh.uthconnect.domain.model.Event
import com.giathinh.uthconnect.domain.model.EventStatus
import com.giathinh.uthconnect.domain.model.HomeBanner
import com.giathinh.uthconnect.domain.model.HousingListing
import com.giathinh.uthconnect.domain.model.HousingListingStatus
import com.giathinh.uthconnect.domain.model.LostFoundListing

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
    val events: List<Event> = emptyList(),
    val registeredEventIds: Set<String> = emptySet(),
    /** `null` = chip "Tất cả". */
    val selectedCategory: AnnouncementCategory? = null,
    /** `null` = "Tất cả cơ sở". */
    val selectedCampus: String? = null,
    val housingListings: List<HousingListing> = emptyList(),
    val bookListings: List<BookListing> = emptyList(),
    val lostFoundListings: List<LostFoundListing> = emptyList(),
    val failure: HomeFailure? = null,
) {

    /** Danh sách sự kiện đã lọc theo SQL status, thời gian và bộ lọc cơ sở/danh mục. */
    val visibleEvents: List<Event>
        get() {
            if (selectedCategory != null && selectedCategory != AnnouncementCategory.YOUTH_UNION) {
                return emptyList()
            }
            val now = System.currentTimeMillis()
            return events
                .filter { it.status == EventStatus.PUBLISHED || it.status == EventStatus.HAPPENING }
                .filter { it.endAt >= now }
                .filter { selectedCampus.isNullOrBlank() || selectedCampus == "Tất cả" || it.campus == selectedCampus }
                .sortedBy { it.startAt }
        }

    /** Danh sách tin ghép trọ đang mở hiển thị trên trang chủ. */
    val visibleHousingListings: List<HousingListing>
        get() {
            if (selectedCategory != null) return emptyList()
            return housingListings
                .filter { it.status == HousingListingStatus.OPEN }
                .take(5)
        }

    /** Danh sách sách giáo trình còn bán / trao đổi hiển thị trên trang chủ. */
    val visibleBookListings: List<BookListing>
        get() {
            if (selectedCategory != null) return emptyList()
            return bookListings
                .filter { it.status == BookListingStatus.AVAILABLE }
                .take(5)
        }

    /** Danh sách đồ thất lạc chưa giải quyết hiển thị trên trang chủ. */
    val visibleLostFoundListings: List<LostFoundListing>
        get() {
            if (selectedCategory != null) return emptyList()
            return lostFoundListings
                .filter { !it.isResolved }
                .take(5)
        }

    /** Bảng tin đã áp bộ lọc nhóm — Screen chỉ việc render danh sách này. */
    val visibleAnnouncements: List<Announcement>
        get() = selectedCategory
            ?.let { selected -> announcements.filter { it.category == selected } }
            ?: announcements

    /** Rỗng = đã tải xong, không lỗi và toàn bộ nội dung đều không có. */
    val isEmpty: Boolean
        get() = !isLoading && failure == null &&
            visibleAnnouncements.isEmpty() &&
            visibleEvents.isEmpty() &&
            visibleHousingListings.isEmpty() &&
            visibleBookListings.isEmpty() &&
            visibleLostFoundListings.isEmpty()

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
