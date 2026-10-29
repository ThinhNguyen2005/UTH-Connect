package com.giathinh.uthconnect.presentation.utilities

import com.giathinh.uthconnect.domain.model.UtilityModule

/**
 * Trạng thái hiển thị của tab Tiện ích. Một `data class` bất biến, mọi tham số
 * có giá trị mặc định = trạng thái an toàn nhất khi Screen vừa mở.
 *
 * Quy ước cho cả nhóm:
 * - UiState không chứa `@StringRes`, không chứa `Modifier`/`Context` — ViewModel
 *   không được cầm tài nguyên UI (xem `README.md` mục 5.2).
 * - Thất bại được mô tả bằng enum; UI map enum đó sang chuỗi trong `strings.xml`.
 * - Không thêm trường dẫn đường (`navigatedTo`) vào UiState.
 */
data class UtilitiesUiState(
    val isLoading: Boolean = true,
    val modules: List<UtilityModule> = emptyList(),
    val failure: UtilitiesFailure? = null,
) {
    val isEmpty: Boolean
        get() = !isLoading && failure == null && modules.isEmpty()
}

/** Lý do thất bại, đủ để UI chọn thông điệp và CTA. */
enum class UtilitiesFailure {
    /** Không đọc được danh sách module. */
    LoadFailed,
}
