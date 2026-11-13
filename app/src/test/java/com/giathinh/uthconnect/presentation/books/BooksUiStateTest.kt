package com.giathinh.uthconnect.presentation.books

import com.giathinh.uthconnect.domain.model.BookCondition
import com.giathinh.uthconnect.domain.model.BookDealType
import com.giathinh.uthconnect.domain.model.BookListing
import com.giathinh.uthconnect.domain.model.BookListingStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class BooksUiStateTest {
    @Test
    fun `query and filters hide sold and unrelated listings`() {
        val kotlinBook = listing("Kotlin for Android", BookDealType.SALE)
        val soldBook = listing("Java Basics", BookDealType.SALE, BookListingStatus.SOLD)
        val exchangeBook = listing("Kotlin Notes", BookDealType.EXCHANGE)

        val state = BooksUiState(
            isLoading = false,
            listings = listOf(kotlinBook, soldBook, exchangeBook),
            query = "kotlin",
            filter = BooksFilter.SALE,
        )

        assertEquals(listOf(kotlinBook), state.visibleListings)
    }

    @Test
    fun `price range excludes exchanges without a price`() {
        val cheap = listing("Cheap", BookDealType.SALE).copy(priceVnd = 1000)
        val expensive = listing("Expensive", BookDealType.SALE).copy(priceVnd = 5000)
        val exchange = listing("Exchange", BookDealType.EXCHANGE).copy(priceVnd = null)

        val state = BooksUiState(
            isLoading = false,
            listings = listOf(cheap, expensive, exchange),
            minimumPrice = "900",
            maximumPrice = "2000",
        )

        assertEquals(listOf(cheap), state.visibleListings)
    }

    private fun listing(title: String, deal: BookDealType, status: BookListingStatus = BookListingStatus.AVAILABLE) = BookListing(
        id = "$title-$deal", title = title, subject = "CS", condition = BookCondition.GOOD,
        dealType = deal, priceVnd = 1000, description = "", sellerName = "Seller",
        contact = "0901234567", status = status,
    )
}
