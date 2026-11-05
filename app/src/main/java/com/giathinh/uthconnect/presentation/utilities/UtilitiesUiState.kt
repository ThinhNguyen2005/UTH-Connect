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

/**
 * Event một lần của tab Tiện ích — thu bằng `LaunchedEffect` trong Route
 * (`docs/CODING_STANDARDS.md` mục 6), không nhét vào UiState vì UiState tái tạo
 * liên tục còn event chỉ được xử lý một lần.
 */
sealed interface UtilitiesEvent {
    /** Ngôn ngữ vừa đổi: Locale chỉ áp được khi activity dựng lại tài nguyên. */
    data object LanguageChanged : UtilitiesEvent
}
