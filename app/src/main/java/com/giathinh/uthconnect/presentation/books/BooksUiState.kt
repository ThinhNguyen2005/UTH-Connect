package com.giathinh.uthconnect.presentation.books

import com.giathinh.uthconnect.domain.model.BookCondition
import com.giathinh.uthconnect.domain.model.BookDealType
import com.giathinh.uthconnect.domain.model.BookListing
import com.giathinh.uthconnect.domain.model.BookListingStatus

enum class BooksFailure {
    LOAD_FAILED,
    SAVE_FAILED,
    DELETE_FAILED,
    UPDATE_STATUS_FAILED,
}

data class BooksUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val mutatingIds: Set<String> = emptySet(),
    val currentUserUid: String? = null,
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
            (includeSold || listing.status == BookListingStatus.AVAILABLE) &&
                (
                    filter == BooksFilter.ALL || (filter == BooksFilter.SALE && listing.dealType == BookDealType.SALE) ||
                        (filter == BooksFilter.EXCHANGE && listing.dealType == BookDealType.EXCHANGE)
                    ) &&
                (condition == null || listing.condition == condition) &&
                (minimumPrice.toLongOrNull()?.let { minimum -> listing.priceVnd?.let { it >= minimum } ?: false } != false) &&
                (maximumPrice.toLongOrNull()?.let { maximum -> listing.priceVnd?.let { it <= maximum } ?: false } != false) &&
                (
                    query.isBlank() || listOf(listing.title, listing.subject, listing.description)
                        .any { it.contains(query.trim(), ignoreCase = true) }
                    )
        }.let { matches ->
            when (sort) {
                BooksSort.NEWEST -> matches.sortedByDescending { it.createdAt }
                BooksSort.PRICE_LOW -> matches.sortedBy { it.priceVnd ?: Long.MAX_VALUE }
                BooksSort.PRICE_HIGH -> matches.sortedByDescending { it.priceVnd ?: Long.MIN_VALUE }
            }
        }
}
