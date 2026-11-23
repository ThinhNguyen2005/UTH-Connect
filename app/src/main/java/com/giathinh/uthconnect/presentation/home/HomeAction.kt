package com.giathinh.uthconnect.presentation.home

import com.giathinh.uthconnect.domain.model.AnnouncementCategory

/**
 * Ý định của người dùng trên tab Trang chủ — một chiều vào ViewModel.
 *
 * Dẫn đường giữa các tab không nằm ở đây (việc của `presentation/navigation/`); các action
 * mở màn hình con sẽ do branch dựng graph của module Sự kiện nối vào sau.
 */
sealed interface HomeAction {

    /** Bấm "Thử lại" sau khi nạp bảng tin thất bại. */
    data object Retry : HomeAction

    /** Bấm chuông thông báo ở header. */
    data object OpenNotifications : HomeAction

    /** Bấm "Bộ lọc" cạnh tiêu đề bảng tin (bộ lọc nâng cao, chưa có màn hình). */
    data object OpenFilter : HomeAction

    /** Bấm "Xem thêm lưu trữ" ở cuối bảng tin. */
    data object OpenArchive : HomeAction

    /**
     * Chọn một chip nhóm. `category = null` là chip "Tất cả" — bỏ lọc.
     *
     * Lọc chạy trong `HomeUiState`, không chạy ở Composable: Screen chỉ render kết quả.
     */
    data class SelectCategory(val category: AnnouncementCategory?) : HomeAction

    /** Mở chi tiết một thông báo hoặc banner. */
    data class OpenAnnouncement(val announcementId: String) : HomeAction

    /** Chọn cơ sở cho khối Sự kiện. `campus = null` hoặc `"Tất cả"` là xem toàn trường. */
    data class SelectCampus(val campus: String?) : HomeAction

    /** Mở xem chi tiết sự kiện. */
    data class OpenEvent(val eventId: String) : HomeAction

    /** Bấm "Xem tất cả" mở màn hình danh sách sự kiện đầy đủ. */
    data object OpenAllEvents : HomeAction

    /** Mở chi tiết tin ghép trọ. */
    data class OpenHousingDetail(val listingId: String) : HomeAction

    /** Bấm "Xem tất cả" mở màn hình ghép trọ. */
    data object OpenAllHousing : HomeAction

    /** Bấm nút tạo tin đăng ghép trọ mới. */
    data object CreateHousingPost : HomeAction

    /** Mở chi tiết sách trong chợ sách giáo trình. */
    data class OpenBookDetail(val bookId: String) : HomeAction

    /** Bấm "Xem tất cả" mở màn hình chợ sách. */
    data object OpenAllBooks : HomeAction

    /** Mở chi tiết đồ thất lạc. */
    data class OpenLostFoundDetail(val itemId: String) : HomeAction

    /** Bấm "Xem tất cả" mở màn hình đồ thất lạc. */
    data object OpenAllLostFound : HomeAction

    /** Bấm nút đăng tin đồ thất lạc mới. */
    data object CreateLostFoundPost : HomeAction
}
