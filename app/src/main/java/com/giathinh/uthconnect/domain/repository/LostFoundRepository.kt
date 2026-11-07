package com.giathinh.uthconnect.domain.repository

import com.giathinh.uthconnect.domain.model.LostFoundListing

/**
 * Repository interface governing lost and found listing operations.
 */
interface LostFoundRepository {
    /**
     * Retrieves all active and resolved lost and found listings.
     */
    suspend fun getListings(): Result<List<LostFoundListing>>

    /**
     * Reports a new lost or found item with optional photo attachment.
     */
    suspend fun reportItem(
        item: LostFoundListing,
        imageBytes: ByteArray? = null,
        imageMimeType: String? = "image/jpeg",
    ): Result<LostFoundListing>

    /**
     * Toggles whether a listing is marked as resolved/recovered.
     */
    suspend fun toggleResolved(id: String, isResolved: Boolean): Result<Unit>
}
