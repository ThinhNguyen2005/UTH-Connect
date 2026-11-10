package com.giathinh.uthconnect.presentation.books

import com.giathinh.uthconnect.domain.model.BookCondition
import com.giathinh.uthconnect.domain.model.BookDealType
import com.giathinh.uthconnect.domain.model.BookListing
import com.giathinh.uthconnect.domain.model.BookListingStatus
import com.giathinh.uthconnect.domain.repository.BookListingRepository
import com.giathinh.uthconnect.domain.usecase.BookListingUseCases
import com.giathinh.uthconnect.testing.MainDispatcherRule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class BooksViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `failed save reports error and clears saving state`() {
        val viewModel = BooksViewModel(BookListingUseCases(FailingSaveRepository))

        viewModel.onAction(
            BooksAction.Save(
                BookListing(
                    id = "book-1", title = "Kotlin", subject = "Programming", condition = BookCondition.GOOD,
                    dealType = BookDealType.SALE, priceVnd = 1000, description = "", sellerName = "Student",
                    contact = "student@example.edu",
                ),
            ),
        )

        assertFalse(viewModel.uiState.value.isSaving)
        assertEquals(BooksFailure.SAVE_FAILED, viewModel.uiState.value.saveFailure)
        assertEquals(BooksFailure.SAVE_FAILED, viewModel.uiState.value.failure)
    }

    @Test
    fun `load update does not clear save failure`() {
        val stream = MutableStateFlow<List<BookListing>>(emptyList())
        val repository = object : BookListingRepository {
            override fun currentUserId() = "student-1"
            override fun observeListings(): Flow<List<BookListing>> = stream
            override suspend fun saveListing(listing: BookListing) = Result.failure<Unit>(IllegalStateException("offline"))
            override suspend fun updateStatus(id: String, status: BookListingStatus) = Result.success(Unit)
            override suspend fun deleteListing(id: String) = Result.success(Unit)
            override fun refresh() = Unit
        }
        val viewModel = BooksViewModel(BookListingUseCases(repository))

        viewModel.onAction(
            BooksAction.Save(
                BookListing(
                    id = "book-1", title = "Kotlin", subject = "Programming", condition = BookCondition.GOOD,
                    dealType = BookDealType.SALE, priceVnd = 1000, description = "", sellerName = "Student",
                    contact = "student@example.edu",
                ),
            ),
        )

        assertEquals(BooksFailure.SAVE_FAILED, viewModel.uiState.value.saveFailure)

        // New snapshot from stream should update listings but NOT wipe out saveFailure
        val newListing = BookListing(
            id = "book-2", title = "Algorithms", subject = "CS", condition = BookCondition.NEW,
            dealType = BookDealType.EXCHANGE, priceVnd = null, description = "", sellerName = "Peer",
            contact = "0901234567",
        )
        stream.value = listOf(newListing)

        assertEquals(1, viewModel.uiState.value.listings.size)
        assertEquals(BooksFailure.SAVE_FAILED, viewModel.uiState.value.saveFailure)
    }

    @Test
    fun `setStatus updates status via dedicated call without resending full listing`() {
        var updatedId: String? = null
        var updatedStatus: BookListingStatus? = null

        val repository = object : BookListingRepository {
            override fun currentUserId() = "student-1"
            override fun observeListings(): Flow<List<BookListing>> = flowOf(emptyList())
            override suspend fun saveListing(listing: BookListing) = Result.success(Unit)
            override suspend fun updateStatus(id: String, status: BookListingStatus): Result<Unit> {
                updatedId = id
                updatedStatus = status
                return Result.success(Unit)
            }
            override suspend fun deleteListing(id: String) = Result.success(Unit)
            override fun refresh() = Unit
        }

        val viewModel = BooksViewModel(BookListingUseCases(repository))
        val sample = BookListing(
            id = "book-42", title = "Network", subject = "IT", condition = BookCondition.GOOD,
            dealType = BookDealType.SALE, priceVnd = 50000, description = "desc", sellerName = "Me",
            contact = "0912345678", status = BookListingStatus.AVAILABLE,
        )

        viewModel.onAction(BooksAction.SetStatus(sample, BookListingStatus.SOLD))

        assertEquals("book-42", updatedId)
        assertEquals(BookListingStatus.SOLD, updatedStatus)
        assertTrue(viewModel.uiState.value.mutatingIds.isEmpty())
        assertNull(viewModel.uiState.value.saveFailure)
    }

    private object FailingSaveRepository : BookListingRepository {
        override fun currentUserId() = "student-1"
        override fun observeListings(): Flow<List<BookListing>> = flowOf(emptyList())
        override suspend fun saveListing(listing: BookListing) = Result.failure<Unit>(IllegalStateException("offline"))
        override suspend fun updateStatus(id: String, status: BookListingStatus) = Result.success(Unit)
        override suspend fun deleteListing(id: String) = Result.success(Unit)
        override fun refresh() = Unit
    }
}
