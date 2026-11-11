package com.giathinh.uthconnect.presentation.housing

import com.giathinh.uthconnect.domain.model.GenderPreference
import com.giathinh.uthconnect.domain.model.HousingListingType
import com.giathinh.uthconnect.domain.model.HousingPriceRange
import com.giathinh.uthconnect.domain.model.UthCampus

/** Ý định của người dùng trên màn Ghép trọ — Screen phát, ViewModel xử lý. */
sealed interface HousingAction {
    /** Tìm nhanh theo tiêu đề / địa chỉ; chuỗi rỗng để xóa tìm kiếm. */
    data class Search(val query: String) : HousingAction

    /** Lưu / bỏ lưu tin bằng tim — toggle in-memory trong phiên. */
    data class ToggleSaved(val id: String) : HousingAction

    /** Gọi nhanh chủ tin từ card feed — Route nhận event rồi mở trình quay số. */
    data class CallHost(val listingId: String) : HousingAction

    /** `null` = "Tất cả", nên tham số để nullable chứ không thêm giá trị giả vào enum. */
    data class SelectType(val type: HousingListingType?) : HousingAction

    data class SelectCampus(val campus: UthCampus?) : HousingAction
    data class SelectGender(val gender: GenderPreference?) : HousingAction
    data class SelectPriceRange(val range: HousingPriceRange) : HousingAction

    /** `null` = không chặn theo giá. */
    data class SelectMaxRent(val monthlyRentVnd: Int?) : HousingAction
    data class SelectMaxDistance(val meters: Int?) : HousingAction
    data class SelectSort(val sort: HousingSort) : HousingAction
    data object ClearFilters : HousingAction
    data object OpenFilters : HousingAction
    data object CloseFilters : HousingAction

    /** Mở dialog với lý do chưa chọn — gửi một lý do đại diện sẽ khiến backend xử phạt nhầm chủ tin. */
    data class OpenReport(val listingId: String) : HousingAction

    /** Lý do báo vi phạm chọn từ enum, không nhập tự do — backend cần nhóm được lý do. */
    data class SelectReportReason(val reason: HousingReportReason) : HousingAction
    data object CloseReport : HousingAction
    data object ConfirmReport : HousingAction
    data object Retry : HousingAction

    /** Nạp lại nền khi màn hình resume (đăng tin xong quay về) — không nháy spinner. */
    data object Refresh : HousingAction
}

/**
 * Lý do báo tin. Map enum → chuỗi nằm ở Screen (docs/CODING_STANDARDS.md mục 3.3:
 * ViewModel cầm `@StringRes` là không đổi được ngôn ngữ và không test được).
 */
enum class HousingReportReason {
    FAKE_INFO,
    BROKER_SPAM,
    WRONG_PRICE,
    ALREADY_FULL,
    OFFENSIVE_CONTENT,
}
