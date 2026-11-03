package com.giathinh.uthconnect.domain.repository

import com.giathinh.uthconnect.domain.model.BookListing
import kotlinx.coroutines.flow.Flow

interface BookListingRepository {
    fun currentUserId(): String?
    fun observeListings(): Flow<List<BookListing>>
    suspend fun saveListing(listing: BookListing): Result<Unit>
    suspend fun deleteListing(id: String): Result<Unit>
}
