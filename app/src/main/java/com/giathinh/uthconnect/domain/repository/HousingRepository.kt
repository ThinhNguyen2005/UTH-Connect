package com.giathinh.uthconnect.domain.repository

import com.giathinh.uthconnect.domain.model.HousingListing

interface HousingRepository {
    suspend fun getListings(): Result<List<HousingListing>>

    suspend fun getListingById(id: String): Result<HousingListing>

    suspend fun createListing(listing: HousingListing): Result<HousingListing>

    suspend fun updateListing(listing: HousingListing): Result<HousingListing>

    suspend fun markListingFulfilled(id: String): Result<Unit>

    suspend fun deleteListing(id: String): Result<Unit>

    /**
     * Báo tin ảo/sai sự thật. Backend quyết định ẩn tin khi đủ ngưỡng
     * (README 3.2: 3 báo cáo) — client không tự đổi `status` được.
     */
    suspend fun reportListing(listingId: String, reason: String): Result<Unit>
}
