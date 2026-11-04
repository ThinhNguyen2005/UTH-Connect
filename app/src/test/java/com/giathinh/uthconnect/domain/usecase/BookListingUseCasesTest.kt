package com.giathinh.uthconnect.domain.usecase

import com.giathinh.uthconnect.domain.model.BookCondition
import com.giathinh.uthconnect.domain.model.BookDealType
import com.giathinh.uthconnect.domain.model.BookListing
import com.giathinh.uthconnect.domain.repository.BookListingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BookListingUseCasesTest {
    @Test
    fun `sale without positive price is rejected before persistence`() = runTest {
        val repository = FakeBookListingRepository()
        val result = BookListingUseCases(repository).save(listing(price = null))

        assertTrue(result.isFailure)
        assertFalse(repository.saved)
    }

    @Test
    fun `valid exchange listing does not require a price`() = runTest {
        val repository = FakeBookListingRepository()
        val result = BookListingUseCases(repository).save(listing(dealType = BookDealType.EXCHANGE, price = null))

        assertTrue(result.isSuccess)
        assertTrue(repository.saved)
    }

    @Test
    fun `exchange listing with a price is rejected before persistence`() = runTest {
        val repository = FakeBookListingRepository()
        val result = BookListingUseCases(repository).save(listing(dealType = BookDealType.EXCHANGE, price = 1000))

        assertTrue(result.isFailure)
        assertFalse(repository.saved)
    }

    @Test
    fun `invalid contact is rejected`() = runTest {
        val repository = FakeBookListingRepository()
        val result = BookListingUseCases(repository).save(listing(contact = "not contact"))

        assertTrue(result.isFailure)
        assertFalse(repository.saved)
    }

    @Test
    fun `subject over 100 characters is rejected before persistence`() = runTest {
        val repository = FakeBookListingRepository()
        val result = BookListingUseCases(repository).save(listing().copy(subject = "x".repeat(101)))

        assertTrue(result.isFailure)
        assertFalse(repository.saved)
    }

    private fun listing(
        dealType: BookDealType = BookDealType.SALE,
        price: Long? = 1000,
        contact: String = "0901234567",
    ) = BookListing(
        id = "book-1",
        title = "Kotlin",
        subject = "Mobile development",
        condition = BookCondition.GOOD,
        dealType = dealType,
        priceVnd = price,
        description = "",
        sellerName = "Student",
        contact = contact,
    )

    private class FakeBookListingRepository : BookListingRepository {
        var saved = false
        override fun currentUserId() = "test-user"
        override fun observeListings(): Flow<List<BookListing>> = flowOf(emptyList())
        override suspend fun saveListing(listing: BookListing): Result<Unit> {
            saved = true
            return Result.success(Unit)
        }
        override suspend fun deleteListing(id: String): Result<Unit> = Result.success(Unit)
    }
}
