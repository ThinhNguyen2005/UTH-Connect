package com.giathinh.uthconnect.presentation.announcement

import androidx.compose.runtime.Immutable
import com.giathinh.uthconnect.domain.model.AnnouncementDetail

/**
 * Trạng thái hiển thị của màn hình Chi tiết — mọi tham số có giá trị mặc định là trạng
 * thái an toàn nhất lúc màn hình vừa mở (`docs/CODING_STANDARDS.md` mục 3.3).
 *
 * Ba trạng thái cuối loại trừ nhau và suy ra được từ hai field, nên Screen không phải tự đoán:
 * đang tải → [isLoading]; lỗi đọc → [failure]; không tìm thấy bài → [isMissing]; còn lại là
 * [detail].
 *
 * @Immutable: Compose bỏ qua recomposition khi tham chiếu không đổi (mục 5).
 */
@Immutable
data class AnnouncementUiState(
    val isLoading: Boolean = true,
    val detail: AnnouncementDetail? = null,
    val failure: AnnouncementFailure? = null,
) {

    /**
     * Đã tải xong, không lỗi, nhưng không có bài → đường dẫn sai hoặc bài đã bị gỡ.
     * Khác hẳn với lỗi mạng: lối thoát là quay về, không phải "Thử lại".
     */
    val isMissing: Boolean
        get() = !isLoading && failure == null && detail == null
}

/** Lý do thất bại, đủ để UI chọn thông điệp và CTA. */
enum class AnnouncementFailure {

    /** Không đọc được nội dung thông báo từ repository. */
    LoadFailed,
}
