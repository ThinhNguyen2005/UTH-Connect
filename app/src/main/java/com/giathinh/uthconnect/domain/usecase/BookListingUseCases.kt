package com.giathinh.uthconnect.domain.usecase

import com.giathinh.uthconnect.domain.model.BookDealType
import com.giathinh.uthconnect.domain.model.BookListing
import com.giathinh.uthconnect.domain.model.BookListingStatus
import com.giathinh.uthconnect.domain.repository.BookListingRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

private const val MAX_TITLE_LENGTH = 160
private const val MAX_SUBJECT_LENGTH = 100
private const val MAX_SELLER_NAME_LENGTH = 100
private const val MAX_DESCRIPTION_LENGTH = 2000
private const val MAX_CONTACT_LENGTH = 254
private const val MIN_PHONE_DIGITS = 8

class BookListingUseCases @Inject constructor(private val repository: BookListingRepository) {
    fun currentUserId(): String? = repository.currentUserId()
    fun observeListings(): Flow<List<BookListing>> = repository.observeListings()
    fun refresh() = repository.refresh()

    suspend fun save(listing: BookListing): Result<Unit> {
        val invalid = when {
            listing.id.isBlank() || listing.title.isBlank() || listing.title.length > MAX_TITLE_LENGTH -> true

            listing.subject.isBlank() || listing.subject.length > MAX_SUBJECT_LENGTH ||
                listing.sellerName.isBlank() -> true

            listing.sellerName.length > MAX_SELLER_NAME_LENGTH ||
                listing.description.length > MAX_DESCRIPTION_LENGTH ||
                listing.contact.length > MAX_CONTACT_LENGTH -> true

            !isValidContact(listing.contact) -> true

            listing.dealType == BookDealType.SALE && (listing.priceVnd ?: 0) <= 0 -> true

            listing.dealType == BookDealType.EXCHANGE && listing.priceVnd != null -> true

            else -> false
        }
        if (invalid) {
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
            (contact.matches(Regex("^[+()0-9\\s-]{8,20}$")) && contact.filter(Char::isDigit).length >= MIN_PHONE_DIGITS)
    }
}
