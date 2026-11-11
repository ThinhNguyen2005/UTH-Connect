package com.giathinh.uthconnect.domain.repository

import com.giathinh.uthconnect.domain.model.HousingContactRequest
import com.giathinh.uthconnect.domain.model.HousingListing
import com.giathinh.uthconnect.domain.model.HousingPrivateDetails

interface HousingRepository {
    suspend fun getListings(): Result<List<HousingListing>>

    suspend fun getListingById(id: String): Result<HousingListing>

    /** Ghi dòng public và [privateDetails] trong một transaction phía backend. */
    suspend fun createListing(listing: HousingListing, privateDetails: HousingPrivateDetails): Result<HousingListing>

    suspend fun updateListing(listing: HousingListing, privateDetails: HousingPrivateDetails): Result<HousingListing>

    suspend fun markListingFulfilled(id: String): Result<Unit>

    suspend fun deleteListing(id: String): Result<Unit>

    /**
     * Báo tin ảo/sai sự thật. Backend quyết định ẩn tin khi đủ ngưỡng
     * (README 3.2: 3 báo cáo) — client không tự đổi `status` được.
     */
    suspend fun reportListing(listingId: String, reason: String): Result<Unit>

    /** Gửi yêu cầu liên hệ; trả id request (đã PENDING thì trả lại id cũ). */
    suspend fun requestContact(listingId: String, message: String): Result<String>

    /** Chủ tin chấp nhận hoặc từ chối một request PENDING. */
    suspend fun respondContact(requestId: String, accept: Boolean): Result<Unit>

    /** Người hỏi rút request PENDING của mình. */
    suspend fun cancelContact(requestId: String): Result<Unit>

    /** Phía chủ tin: mọi request của tin, mới nhất trước. */
    suspend fun getContactRequests(listingId: String): Result<List<HousingContactRequest>>

    /** Phía người hỏi: request gần nhất của mình với tin, null nếu chưa hỏi. */
    suspend fun getMyContactRequest(listingId: String): Result<HousingContactRequest?>

    /** Tọa độ chính xác + liên hệ; backend chỉ trả cho chủ tin hoặc requester ACCEPTED. */
    suspend fun revealPrivateDetails(listingId: String): Result<HousingPrivateDetails>
}
