package com.giathinh.uthconnect.presentation.housing

import com.giathinh.uthconnect.domain.model.GenderPreference
import com.giathinh.uthconnect.domain.model.HousingListing
import com.giathinh.uthconnect.domain.model.HousingListingType
import com.giathinh.uthconnect.domain.model.HousingPriceRange
import com.giathinh.uthconnect.domain.model.UthCampus

/**
 * Sắp xếp feed. Mặc định là mới nhất vì đó là kỳ vọng của một bảng tin trọ;
 * "gần cơ sở" chỉ có nghĩa khi người dùng đã chọn cơ sở hoặc đang học ở đó.
 */
enum class HousingSort {
    NEWEST,
    NEAREST_CAMPUS,
    CHEAPEST,
}

/**
 * Lỗi chặn luồng tải feed, dạng enum — Screen quyết định in câu nào
 * (docs/CODING_STANDARDS.md mục 3.3). Kết quả báo tin không nằm ở đây mà đi qua
 * kênh event một lần trong ViewModel (mục 6), vì không chặn người dùng đọc feed.
 */
enum class HousingFailure {
    LoadFailed,
}

data class HousingUiState(
    /** Mặc định `true` để màn vừa mở không nháy câu "chưa có tin trọ" (mục 3.3). */
    val isLoading: Boolean = true,
    val listings: List<HousingListing> = emptyList(),

    /** Từ khóa tìm nhanh theo tiêu đề / địa chỉ (không phân biệt hoa thường). Rỗng = không tìm. */
    val searchQuery: String = "",

    /**
     * Tin đã lưu tim — giữ trong phiên (in-memory) vì chưa có backend lưu tin.
     * Tách khỏi [listings] để toggle không phải dựng lại danh sách feed.
     */
    val savedListingIds: Set<String> = emptySet(),

    /** Bộ lọc — `null` ở mọi ô có nghĩa "Tất cả", trạng thái an toàn nhất lúc mới mở màn. */
    val selectedType: HousingListingType? = null,
    val selectedCampus: UthCampus? = null,
    val priceRange: HousingPriceRange = HousingPriceRange.ALL,
    val maxRentVnd: Int? = null,
    val maxDistanceM: Int? = null,
    val genderPreference: GenderPreference? = null,
    val sort: HousingSort = HousingSort.NEWEST,
    val isFilterDialogOpen: Boolean = false,

    /**
     * Tin + lý do đang được báo vi phạm. `reportingReason` mặc định `null` để nút Gửi
     * trong dialog tắt cho tới khi người dùng chọn hẳn lý do — gửi một lý do đại diện
     * sẽ khiến backend xử phạt nhầm chủ tin.
     */
    val reportingListingId: String? = null,
    val reportingReason: HousingReportReason? = null,
    val failure: HousingFailure? = null,
) {
    /**
     * Feed hiển thị thật. Lọc đặt trong `UiState` chứ không phải một `StateFlow` phụ
     * song song (docs/CODING_STANDARDS.md mục 6: một nguồn sự thật mỗi màn hình) và
     * cũng không đặt trong Screen (bài học tab Thời khóa biểu: lọc trong Screen thì
     * JVM test không với tới).
     *
     * Lọc in-memory đáp ứng tiêu chí nghiệm thu `README.md` mục 3.2 (500 tin < 300 ms);
     * khi có backend thì đẩy bộ lọc xuống query, vẫn giữ tên thuộc tính này.
     */
    val filteredListings: List<HousingListing>
        get() {
            val kept = listings.filter { it.matches(this) && it.matchesQuery(searchQuery) }
            return when (sort) {
                HousingSort.NEWEST -> kept

                HousingSort.NEAREST_CAMPUS -> kept.sortedWith(
                    compareBy({ !it.hasLocation }, { it.distanceToCampusM }),
                )

                HousingSort.CHEAPEST -> kept.sortedBy { it.monthlyRentVnd }
            }
        }

    val hasActiveFilters: Boolean
        get() = searchQuery.isNotBlank() || selectedType != null || selectedCampus != null ||
            priceRange != HousingPriceRange.ALL || maxRentVnd != null || maxDistanceM != null ||
            genderPreference != null

    /** Nuôi dòng "N tin đang mở" trên header — đếm theo tin publishable, không theo danh sách thô. */
    val visibleCount: Int get() = filteredListings.size

    /**
     * loading ≠ empty: nếu gộp hai cái thành một thì khung skeleton kịp biến mất
     * trước khi dữ liệu về và người dùng thấy "chưa có tin" trong vài trăm ms.
     */
    val isEmpty: Boolean get() = !isLoading && failure == null && filteredListings.isEmpty()

    /** Rỗng vì lọc, khác rỗng vì chưa ai đăng — hai câu hướng dẫn khác nhau trên UI (mục 4.9). */
    val isEmptyBecauseOfFilters: Boolean get() = isEmpty && hasActiveFilters

    /** Dialog báo vi phạm chỉ mở khi đã chọn được cả tin lẫn lý do. */
    val isReportDialogOpen: Boolean get() = reportingListingId != null
}

/** Nhận cả [HousingUiState] để gom bộ lọc về một tham số (tránh LongParameterList). */
private fun HousingListing.matches(state: HousingUiState): Boolean = isPublishable &&
    (state.selectedType == null || type == state.selectedType) &&
    (state.selectedCampus == null || campus == state.selectedCampus) &&
    (state.priceRange == HousingPriceRange.ALL || state.priceRange.matches(monthlyRentVnd)) &&
    (state.maxRentVnd == null || monthlyRentVnd <= state.maxRentVnd) &&
    (state.maxDistanceM == null || (hasLocation && distanceToCampusM <= state.maxDistanceM)) &&
    compatibleWith(genderPreference, state.genderPreference)

/** Tìm nhanh không phân biệt hoa thường trên tiêu đề và địa chỉ — khớp ô search của feed. */
private fun HousingListing.matchesQuery(query: String): Boolean {
    val trimmed = query.trim()
    if (trimmed.isEmpty()) return true
    return title.contains(trimmed, ignoreCase = true) || addressLabel.contains(trimmed, ignoreCase = true)
}

/**
 * Giới hạn giới tính so **hai chiều**: nếu chỉ so một chiều thì tin "chỉ ở được với nam"
 * chui qua bộ lọc "nữ" — bug loại này không ai thấy cho tới khi có người phàn nàn.
 * Chủ tin chưa đặt điều kiện (`ANY`) thì hợp với mọi người tìm.
 */
private fun compatibleWith(hostPreference: GenderPreference, seekerPreference: GenderPreference?): Boolean = when {
    seekerPreference == null || seekerPreference == GenderPreference.ANY -> true
    hostPreference == GenderPreference.ANY -> true
    else -> hostPreference == seekerPreference
}
