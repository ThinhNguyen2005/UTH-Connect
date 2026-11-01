package com.giathinh.uthconnect.domain.usecase

import com.giathinh.uthconnect.domain.model.HomeFeed
import com.giathinh.uthconnect.domain.repository.AnnouncementRepository
import javax.inject.Inject

/**
 * Nạp toàn bộ nội dung tab Trang chủ trong một lượt.
 *
 * Chưa có rule nghiệp vụ nào (lọc theo cơ sở, ghim thông báo, giới hạn số lượng) nên
 * UseCase chỉ chuyển tiếp `Result`. Khi rule xuất hiện, đặt **ở đây** — không đặt trong
 * ViewModel hay Composable.
 */
class GetHomeFeedUseCase @Inject constructor(
    private val repository: AnnouncementRepository,
) {

    suspend operator fun invoke(): Result<HomeFeed> = repository.homeFeed()
}
