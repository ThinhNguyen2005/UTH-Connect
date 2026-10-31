package com.giathinh.uthconnect.domain.repository

import com.giathinh.uthconnect.domain.model.AnnouncementDetail
import com.giathinh.uthconnect.domain.model.HomeFeed

/**
 * Nguồn nội dung thông báo: bảng tin ở tab Trang chủ và nội dung đầy đủ ở màn hình Chi tiết.
 *
 * Interface ở `domain`, implementation ở `data/`; chỉ UseCase được gọi interface này,
 * Composable không được chạm vào (`docs/CODING_STANDARDS.md` mục 3.1).
 *
 * `suspend` + `Result` ngay từ đầu vì nguồn thật là Firestore: bất đồng bộ và có lỗi.
 */
interface AnnouncementRepository {
    suspend fun homeFeed(): Result<HomeFeed>

    /**
     * Nội dung đầy đủ của một thông báo.
     *
     * `success(null)` = **không tìm thấy** (bài bị gỡ, đường dẫn sai) — khác với `failure`
     * (mất mạng). UI cần phân biệt để hiện đúng lối thoát: thử lại hay quay về bảng tin.
     */
    suspend fun detail(id: String): Result<AnnouncementDetail?>
}
