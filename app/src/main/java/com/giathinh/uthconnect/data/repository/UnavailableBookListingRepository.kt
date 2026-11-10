package com.giathinh.uthconnect.data.repository

import com.giathinh.uthconnect.domain.model.BookListing
import com.giathinh.uthconnect.domain.model.BookListingStatus
import com.giathinh.uthconnect.domain.repository.BookListingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class UnavailableBookListingRepository @Inject constructor() : BookListingRepository {
    override fun currentUserId(): String? = null
    override fun observeListings(): Flow<List<BookListing>> = flow {
        throw IllegalStateException("Book marketplace requires authentication")
    }
    override suspend fun saveListing(listing: BookListing): Result<Unit> = Result.failure(IllegalStateException("Book marketplace requires authentication"))
    override suspend fun updateStatus(id: String, status: BookListingStatus): Result<Unit> = Result.failure(IllegalStateException("Book marketplace requires authentication"))
    override suspend fun deleteListing(id: String): Result<Unit> = Result.failure(IllegalStateException("Book marketplace requires authentication"))
    override fun refresh() = Unit
}
