package com.giathinh.uthconnect.presentation.books

import com.giathinh.uthconnect.domain.model.BookCondition
import com.giathinh.uthconnect.domain.model.BookDealType
import com.giathinh.uthconnect.domain.model.BookListing
import com.giathinh.uthconnect.domain.model.BookListingStatus
import com.giathinh.uthconnect.domain.model.StudentProfile

enum class BooksFailure {
    LOAD_FAILED,
    SAVE_FAILED,
    DELETE_FAILED,
    UPDATE_STATUS_FAILED,
}

enum class BookSaveStep {
    IDLE,
    UPLOADING_IMAGE,
    SAVING_LISTING,
    SUCCESS,
}

data class BooksUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val saveStep: BookSaveStep = BookSaveStep.IDLE,
    val mutatingIds: Set<String> = emptySet(),
    val currentUserUid: String? = null,
    val currentProfile: StudentProfile? = null,
    val listings: List<BookListing> = emptyList(),
    val query: String = "",
    val filter: BooksFilter = BooksFilter.ALL,
    val condition: BookCondition? = null,
    val minimumPrice: String = "",
    val maximumPrice: String = "",
    val sort: BooksSort = BooksSort.NEWEST,
    val includeSold: Boolean = false,
    val loadFailure: BooksFailure? = null,
    val saveFailure: BooksFailure? = null,
    val lastSavedId: String? = null,
) {
    /** Backwards-compatibility property for existing code and tests. */
    val failure: BooksFailure? get() = loadFailure ?: saveFailure

    val visibleListings: List<BookListing>
        get() = listings.filter { listing ->
            val matchesSold = includeSold || listing.status == BookListingStatus.AVAILABLE
            val matchesDeal = when (filter) {
                BooksFilter.ALL -> true
                BooksFilter.SALE -> listing.dealType == BookDealType.SALE
                BooksFilter.EXCHANGE -> listing.dealType == BookDealType.EXCHANGE
            }
            val matchesCondition = condition == null || listing.condition == condition
            val min = minimumPrice.toLongOrNull()
            val matchesMin = min == null || (listing.priceVnd != null && listing.priceVnd >= min)
            val max = maximumPrice.toLongOrNull()
            val matchesMax = max == null || (listing.priceVnd != null && listing.priceVnd <= max)
            val matchesQuery = query.isBlank() || listOf(listing.title, listing.subject, listing.description)
                .any { it.contains(query.trim(), ignoreCase = true) }

            matchesSold && matchesDeal && matchesCondition && matchesMin && matchesMax && matchesQuery
        }.let { matches ->
            when (sort) {
                BooksSort.NEWEST -> matches.sortedByDescending { it.createdAt }
                BooksSort.PRICE_LOW -> matches.sortedBy { it.priceVnd ?: Long.MAX_VALUE }
                BooksSort.PRICE_HIGH -> matches.sortedByDescending { it.priceVnd ?: Long.MIN_VALUE }
            }
        }
}
