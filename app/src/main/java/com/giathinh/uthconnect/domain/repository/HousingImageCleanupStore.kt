package com.giathinh.uthconnect.domain.repository

/**
 * Hàng đợi dọn ảnh mồ côi của Ghép trọ, lưu bền vững theo thiết bị. Khi xóa tin
 * mà Storage chưa xóa được ảnh (mất mạng, lỗi tạm thời), path ảnh được ghi vào
 * đây để lần nạp feed/xóa tin kế tiếp thử lại — không mất dấu file rác.
 */
interface HousingImageCleanupStore {
    suspend fun pending(): List<String>
    suspend fun enqueue(paths: List<String>)
    suspend fun dequeue(paths: List<String>)
}
