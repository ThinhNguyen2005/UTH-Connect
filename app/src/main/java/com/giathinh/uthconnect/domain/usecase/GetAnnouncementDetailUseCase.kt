package com.giathinh.uthconnect.domain.usecase

import com.giathinh.uthconnect.domain.model.AnnouncementDetail
import com.giathinh.uthconnect.domain.repository.AnnouncementRepository
import javax.inject.Inject

/**
 * Nạp nội dung đầy đủ của một thông báo cho màn hình Chi tiết.
 *
 * `Result.success(null)` là "không tìm thấy", khác với `Result.failure` (lỗi đọc dữ liệu) —
 * quy ước đó được chốt ở [AnnouncementRepository.detail] và ViewModel dựa vào để chọn trạng thái.
 */
class GetAnnouncementDetailUseCase @Inject constructor(
    private val repository: AnnouncementRepository,
) {

    suspend operator fun invoke(announcementId: String): Result<AnnouncementDetail?> =
        repository.detail(announcementId)
}
