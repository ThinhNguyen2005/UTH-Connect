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
}
