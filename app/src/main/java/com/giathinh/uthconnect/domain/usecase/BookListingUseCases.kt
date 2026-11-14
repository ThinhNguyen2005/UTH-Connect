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
        if (!validateListing(listing)) {
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

    private fun validateListing(listing: BookListing): Boolean {
        if (listing.id.isBlank()) return false
        if (!isTextLengthValid(listing.title, MAX_TITLE_LENGTH)) return false
        if (!isTextLengthValid(listing.subject, MAX_SUBJECT_LENGTH)) return false
        if (!isTextLengthValid(listing.sellerName, MAX_SELLER_LENGTH)) return false
        if (listing.description.length > MAX_DESCRIPTION_LENGTH) return false
        if (listing.contact.length > MAX_CONTACT_LENGTH || !isValidContact(listing.contact)) return false
        return isPriceValid(listing.dealType, listing.priceVnd)
    }

    private fun isTextLengthValid(text: String, maxLength: Int): Boolean = text.isNotBlank() && text.length <= maxLength

    private fun isPriceValid(dealType: BookDealType, priceVnd: Long?): Boolean = when (dealType) {
        BookDealType.SALE -> (priceVnd ?: 0) > 0
        BookDealType.EXCHANGE -> priceVnd == null
    }

    private fun isValidContact(value: String): Boolean {
        val contact = value.trim()
        val isEmail = contact.matches(Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"))
        val isPhone = contact.matches(Regex("^[+()0-9\\s-]{8,20}$")) &&
            contact.filter(Char::isDigit).length >= MIN_PHONE_DIGITS
        return isEmail || isPhone
    }

    companion object {
        private const val MAX_TITLE_LENGTH = 160
        private const val MAX_SUBJECT_LENGTH = 100
        private const val MAX_SELLER_LENGTH = 100
        private const val MAX_DESCRIPTION_LENGTH = 2000
        private const val MAX_CONTACT_LENGTH = 254
        private const val MIN_PHONE_DIGITS = 8
    }
}
