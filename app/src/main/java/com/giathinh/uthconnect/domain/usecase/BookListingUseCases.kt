package com.giathinh.uthconnect.domain.usecase

import com.giathinh.uthconnect.domain.model.BookDealType
import com.giathinh.uthconnect.domain.model.BookListing
import com.giathinh.uthconnect.domain.model.BookListingStatus
import com.giathinh.uthconnect.domain.repository.BookListingRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class BookListingUseCases @Inject constructor(private val repository: BookListingRepository) {
    fun currentUserId(): String? = repository.currentUserId()
    fun observeListings(): Flow<List<BookListing>> = repository.observeListings()
    fun refresh() = repository.refresh()

    suspend fun save(listing: BookListing): Result<Unit> {
        if (listing.id.isBlank() || listing.title.isBlank() || listing.title.length > 160 ||
            listing.subject.isBlank() || listing.subject.length > 100 ||
            listing.sellerName.isBlank() || listing.sellerName.length > 100 ||
            listing.description.length > 2000 || listing.contact.length > 254 ||
            !isValidContact(listing.contact) ||
            (listing.dealType == BookDealType.SALE && (listing.priceVnd ?: 0) <= 0) ||
            (listing.dealType == BookDealType.EXCHANGE && listing.priceVnd != null)
        ) {
            return Result.failure(IllegalArgumentException("Invalid book listing"))
        }
        return repository.saveListing(listing)
    }

    suspend fun updateStatus(id: String, status: BookListingStatus): Result<Unit> {
        if (id.isBlank()) return Result.failure(IllegalArgumentException("Invalid listing ID"))
        return repository.updateStatus(id, status)
    }

    suspend fun delete(id: String): Result<Unit> {
        if (id.isBlank()) return Result.failure(IllegalArgumentException("Invalid listing ID"))
        return repository.deleteListing(id)
    }

    private fun isValidContact(value: String): Boolean {
        val contact = value.trim()
        return contact.matches(Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) ||
            (contact.matches(Regex("^[+()0-9\\s-]{8,20}$")) && contact.filter(Char::isDigit).length >= 8)
    }
}
