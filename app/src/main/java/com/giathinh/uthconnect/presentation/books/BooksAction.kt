package com.giathinh.uthconnect.presentation.books

import com.giathinh.uthconnect.domain.model.BookCondition
import com.giathinh.uthconnect.domain.model.BookListing
import com.giathinh.uthconnect.domain.model.BookListingStatus

sealed interface BooksAction {
    data object Retry : BooksAction
    data object ResetFilters : BooksAction
    data class Search(val value: String) : BooksAction
    data class SetFilter(val value: BooksFilter) : BooksAction
    data class SetCondition(val value: BookCondition?) : BooksAction
    data class SetPriceRange(val minimum: String, val maximum: String) : BooksAction
    data class SetSort(val value: BooksSort) : BooksAction
    data class IncludeSold(val value: Boolean) : BooksAction
    data class Save(val listing: BookListing) : BooksAction
    data class SetStatus(val listing: BookListing, val status: BookListingStatus) : BooksAction
    data class Delete(val id: String) : BooksAction
    data object DismissError : BooksAction
}

enum class BooksFilter { ALL, SALE, EXCHANGE }
enum class BooksSort { NEWEST, PRICE_LOW, PRICE_HIGH }
