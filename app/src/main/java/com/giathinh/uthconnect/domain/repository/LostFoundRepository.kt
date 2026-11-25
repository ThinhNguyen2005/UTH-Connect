package com.giathinh.uthconnect.domain.repository

import com.giathinh.uthconnect.domain.model.LostFoundListing

/**
 * Repository interface governing lost and found listing operations.
 */
interface LostFoundRepository {
    /**
     * Returns the signed-in user's ID, or null when browsing anonymously.
     */
    fun currentUserId(): String? = null

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
     * Updates an existing lost or found item owned by the current user.
     */
    suspend fun updateItem(
        item: LostFoundListing,
        imageBytes: ByteArray? = null,
        imageMimeType: String? = "image/jpeg",
        removeExistingImage: Boolean = false,
    ): Result<LostFoundListing>

    /**
     * Deletes a lost or found item owned by the current user.
     */
    suspend fun deleteItem(id: String, imageUrl: String? = null): Result<Unit>

    /**
     * Toggles whether a listing is marked as resolved/recovered.
     */
    suspend fun toggleResolved(id: String, isResolved: Boolean): Result<Unit>
}
