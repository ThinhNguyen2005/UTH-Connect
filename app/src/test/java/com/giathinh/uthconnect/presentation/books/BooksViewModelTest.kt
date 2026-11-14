package com.giathinh.uthconnect.presentation.books

import com.giathinh.uthconnect.domain.model.BookCondition
import com.giathinh.uthconnect.domain.model.BookDealType
import com.giathinh.uthconnect.domain.model.BookListing
import com.giathinh.uthconnect.domain.model.BookListingStatus
import com.giathinh.uthconnect.domain.model.StudentProfile
import com.giathinh.uthconnect.domain.repository.BookListingRepository
import com.giathinh.uthconnect.domain.repository.SessionRepository
import com.giathinh.uthconnect.domain.repository.StudentSession
import com.giathinh.uthconnect.domain.usecase.BookListingUseCases
import com.giathinh.uthconnect.testing.MainDispatcherRule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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

    @Test
    fun `DismissError clears saveFailure and resets saveStep`() {
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

        assertEquals(BooksFailure.SAVE_FAILED, viewModel.uiState.value.saveFailure)

        viewModel.onAction(BooksAction.DismissError)

        assertNull(viewModel.uiState.value.saveFailure)
        assertEquals(BookSaveStep.IDLE, viewModel.uiState.value.saveStep)
    }

    @Test
    fun `session profile automatically populates currentProfile and currentUserUid`() {
        val sampleProfile = StudentProfile(
            studentId = "20230001",
            email = "student@uth.edu.vn",
            displayName = "Nguyen Van A",
            className = "CNTT1",
            major = "CNTT",
            faculty = "CNTT",
        )
        val sessionRepo = object : SessionRepository {
            override val session: StateFlow<StudentSession> =
                MutableStateFlow(StudentSession(isRestoring = false, profile = sampleProfile))
            override suspend fun restore() = Unit
            override suspend fun start(profile: StudentProfile, remember: Boolean) = Unit
            override suspend fun updateProfile(profile: StudentProfile) = Unit
            override suspend fun enableBiometric(studentId: String) = Unit
            override suspend fun disableBiometric() = Unit
            override suspend fun signOut() = Unit
        }
        val bookRepo = object : BookListingRepository {
            override fun currentUserId(): String? = null
            override fun observeListings(): Flow<List<BookListing>> = flowOf(emptyList())
            override suspend fun saveListing(listing: BookListing) = Result.success(Unit)
            override suspend fun updateStatus(id: String, status: BookListingStatus) = Result.success(Unit)
            override suspend fun deleteListing(id: String) = Result.success(Unit)
            override fun refresh() = Unit
        }

        val viewModel = BooksViewModel(BookListingUseCases(bookRepo), sessionRepo)

        assertEquals(sampleProfile, viewModel.uiState.value.currentProfile)
        assertEquals("20230001", viewModel.uiState.value.currentUserUid)
    }

    @Test
    fun `successful save updates lastSavedId and finishes with success step`() {
        val bookRepo = object : BookListingRepository {
            override fun currentUserId() = "student-1"
            override fun observeListings(): Flow<List<BookListing>> = flowOf(emptyList())
            override suspend fun saveListing(listing: BookListing) = Result.success(Unit)
            override suspend fun updateStatus(id: String, status: BookListingStatus) = Result.success(Unit)
            override suspend fun deleteListing(id: String) = Result.success(Unit)
            override fun refresh() = Unit
        }

        val viewModel = BooksViewModel(BookListingUseCases(bookRepo))
        val sample = BookListing(
            id = "new-book-id", title = "Calculus", subject = "Math", condition = BookCondition.NEW,
            dealType = BookDealType.SALE, priceVnd = 45000, description = "Clean copy",
            sellerName = "Student A", contact = "0987654321",
        )

        viewModel.onAction(BooksAction.Save(sample))

        assertFalse(viewModel.uiState.value.isSaving)
        assertEquals(BookSaveStep.SUCCESS, viewModel.uiState.value.saveStep)
        assertEquals("new-book-id", viewModel.uiState.value.lastSavedId)
        assertNull(viewModel.uiState.value.saveFailure)
    }

    @Test
    fun `failed setStatus reports UPDATE_STATUS_FAILED and clears mutating state`() {
        val repository = object : BookListingRepository {
            override fun currentUserId() = "student-1"
            override fun observeListings(): Flow<List<BookListing>> = flowOf(emptyList())
            override suspend fun saveListing(listing: BookListing) = Result.success(Unit)
            override suspend fun updateStatus(id: String, status: BookListingStatus) = Result.failure<Unit>(IllegalStateException("network error"))
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

        assertEquals(BooksFailure.UPDATE_STATUS_FAILED, viewModel.uiState.value.saveFailure)
        assertTrue(viewModel.uiState.value.mutatingIds.isEmpty())
    }

    @Test
    fun `failed delete reports DELETE_FAILED and clears mutating state`() {
        val repository = object : BookListingRepository {
            override fun currentUserId() = "student-1"
            override fun observeListings(): Flow<List<BookListing>> = flowOf(emptyList())
            override suspend fun saveListing(listing: BookListing) = Result.success(Unit)
            override suspend fun updateStatus(id: String, status: BookListingStatus) = Result.success(Unit)
            override suspend fun deleteListing(id: String) = Result.failure<Unit>(IllegalStateException("network error"))
            override fun refresh() = Unit
        }

        val viewModel = BooksViewModel(BookListingUseCases(repository))

        viewModel.onAction(BooksAction.Delete("book-99"))

        assertEquals(BooksFailure.DELETE_FAILED, viewModel.uiState.value.saveFailure)
        assertTrue(viewModel.uiState.value.mutatingIds.isEmpty())
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
