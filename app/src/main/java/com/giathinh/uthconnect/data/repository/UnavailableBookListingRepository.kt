package com.giathinh.uthconnect.data.repository

import com.giathinh.uthconnect.domain.model.BookListing
import com.giathinh.uthconnect.domain.repository.BookListingRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class UnavailableBookListingRepository @Inject constructor() : BookListingRepository {
    override fun currentUserId(): String? = null
    override fun observeListings(): Flow<List<BookListing>> = flow {
        throw IllegalStateException("Book marketplace requires Firebase authentication")
    }
    override suspend fun saveListing(listing: BookListing): Result<Unit> =
        Result.failure(IllegalStateException("Book marketplace requires Firebase authentication"))
    override suspend fun deleteListing(id: String): Result<Unit> =
        Result.failure(IllegalStateException("Book marketplace requires Firebase authentication"))
}
