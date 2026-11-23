package com.giathinh.uthconnect.presentation.housing.mylistings

import com.giathinh.uthconnect.domain.model.HousingListing

/**
 * Lỗi chặn luồng tải "Tin của tôi", dạng enum — Screen quyết định in câu nào
 * (docs/CODING_STANDARDS.md mục 3.3). Lỗi tải lại nền khi đã có dữ liệu đi qua
 * kênh event một lần trong ViewModel (mục 6), không thay danh sách bằng màn lỗi.
 */
enum class MyListingsFailure {
    LoadFailed,
}

data class MyListingsUiState(
    /** Mặc định `true` để màn vừa mở không nháy câu "chưa đăng tin nào" (mục 3.3). */
    val isLoading: Boolean = true,
    val listings: List<HousingListing> = emptyList(),
    val failure: MyListingsFailure? = null,
) {
    /** loading ≠ empty: gộp hai cái thì người dùng thấy "chưa có tin" trong vài trăm ms trước khi dữ liệu về. */
    val isEmpty: Boolean get() = !isLoading && failure == null && listings.isEmpty()
}
