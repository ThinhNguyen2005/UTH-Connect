package com.giathinh.uthconnect.domain.repository

import com.giathinh.uthconnect.domain.model.LostFoundListing

interface LostFoundRepository {
    suspend fun getListings(): Result<List<LostFoundListing>>
    suspend fun reportItem(
        item: LostFoundListing,
        imageBytes: ByteArray? = null,
        imageMimeType: String? = "image/jpeg",
    ): Result<LostFoundListing>
    suspend fun toggleResolved(id: String, isResolved: Boolean): Result<Unit>
}
