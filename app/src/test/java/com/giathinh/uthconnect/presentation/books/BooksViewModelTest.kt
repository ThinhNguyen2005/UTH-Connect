package com.giathinh.uthconnect.presentation.books

import com.giathinh.uthconnect.domain.model.BookCondition
import com.giathinh.uthconnect.domain.model.BookDealType
import com.giathinh.uthconnect.domain.model.BookListing
import com.giathinh.uthconnect.domain.repository.BookListingRepository
import com.giathinh.uthconnect.domain.usecase.BookListingUseCases
import com.giathinh.uthconnect.testing.MainDispatcherRule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

class BooksViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `failed save reports error and clears saving state`() {
        val viewModel = BooksViewModel(BookListingUseCases(FailingSaveRepository))

        viewModel.onAction(BooksAction.Save(
            BookListing(
                id = "book-1", title = "Kotlin", subject = "Programming", condition = BookCondition.GOOD,
                dealType = BookDealType.SALE, priceVnd = 1000, description = "", sellerName = "Student",
                contact = "student@example.edu",
            ),
        ))

        assertFalse(viewModel.uiState.value.isSaving)
        assertEquals(BooksFailure.SAVE_FAILED, viewModel.uiState.value.failure)
    }

    private object FailingSaveRepository : BookListingRepository {
        override fun currentUserId() = "student-1"
        override fun observeListings(): Flow<List<BookListing>> = flowOf(emptyList())
        override suspend fun saveListing(listing: BookListing) = Result.failure<Unit>(IllegalStateException("offline"))
        override suspend fun deleteListing(id: String) = Result.success(Unit)
    }
}
