package com.giathinh.uthconnect.presentation.announcement

/**
 * Ý định của người dùng trên màn hình Chi tiết thông báo — một chiều vào ViewModel.
 *
 * Quay lại và Chia sẻ **không** nằm ở đây: chúng không đổi trạng thái mà là điều hướng/
 * lối ra hệ thống, nên do `AnnouncementRoute` xử lý (`docs/CODING_STANDARDS.md` mục 3.1).
 */
sealed interface AnnouncementAction {

    /** Bấm "Thử lại" sau khi đọc nội dung thất bại. */
    data object Retry : AnnouncementAction
}
