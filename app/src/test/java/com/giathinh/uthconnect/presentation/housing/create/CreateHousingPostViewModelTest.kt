package com.giathinh.uthconnect.presentation.housing.create

import com.giathinh.uthconnect.domain.model.HousingContactRequest
import com.giathinh.uthconnect.domain.model.HousingListing
import com.giathinh.uthconnect.domain.model.HousingPrivateDetails
import com.giathinh.uthconnect.domain.model.StudentProfile
import com.giathinh.uthconnect.domain.model.UthCampus
import com.giathinh.uthconnect.domain.repository.HousingRepository
import com.giathinh.uthconnect.domain.repository.SessionRepository
import com.giathinh.uthconnect.domain.repository.StudentSession
import com.giathinh.uthconnect.domain.usecase.AccessStudentSessionUseCase
import com.giathinh.uthconnect.domain.usecase.CreateHousingListingUseCase
import com.giathinh.uthconnect.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CreateHousingPostViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private class FakeCreateHousingRepository : HousingRepository {
        var createdListing: HousingListing? = null
        var createdPrivateDetails: HousingPrivateDetails? = null
        private val notFound: Result<HousingListing> = Result.failure(NoSuchElementException())

        override suspend fun getListings(): Result<List<HousingListing>> = Result.success(emptyList())

        override suspend fun getListingById(id: String): Result<HousingListing> = notFound

        override suspend fun createListing(
            listing: HousingListing,
            privateDetails: HousingPrivateDetails,
        ): Result<HousingListing> {
            createdListing = listing
            createdPrivateDetails = privateDetails
            return Result.success(listing)
        }

        override suspend fun updateListing(
            listing: HousingListing,
            privateDetails: HousingPrivateDetails,
        ): Result<HousingListing> = Result.success(listing)

        override suspend fun markListingFulfilled(id: String): Result<Unit> = Result.success(Unit)

        override suspend fun deleteListing(id: String): Result<Unit> = Result.success(Unit)

        override suspend fun reportListing(listingId: String, reason: String): Result<Unit> = Result.success(Unit)

        override suspend fun requestContact(listingId: String, message: String): Result<String> = Result.success("RQ-1")

        override suspend fun respondContact(requestId: String, accept: Boolean): Result<Unit> = Result.success(Unit)

        override suspend fun cancelContact(requestId: String): Result<Unit> = Result.success(Unit)

        override suspend fun getContactRequests(listingId: String): Result<List<HousingContactRequest>> = Result.success(emptyList())

        override suspend fun getMyContactRequest(listingId: String): Result<HousingContactRequest?> = Result.success(null)

        override suspend fun revealPrivateDetails(listingId: String): Result<HousingPrivateDetails> = Result.failure(NoSuchElementException(listingId))
    }

    private class FakeSessionRepository : SessionRepository {
        override val session: StateFlow<StudentSession> = MutableStateFlow(
            StudentSession(
                isRestoring = false,
                profile = StudentProfile(
                    studentId = "2251001234",
                    email = "student@ut.edu.vn",
                    displayName = "Sinh viên kiểm thử",
                    className = "22DTH",
                    major = "CNTT",
                    faculty = "Công nghệ thông tin",
                    uthVerified = true,
                ),
            ),
        )

        override suspend fun restore() = Unit
        override suspend fun start(profile: StudentProfile, remember: Boolean) = Unit
        override suspend fun updateProfile(profile: StudentProfile) = Unit
        override suspend fun enableBiometric(studentId: String) = Unit
        override suspend fun disableBiometric() = Unit
        override suspend fun signOut() = Unit
    }

    private fun viewModel(repository: FakeCreateHousingRepository) = CreateHousingPostViewModel(
        CreateHousingListingUseCase(repository),
        AccessStudentSessionUseCase(FakeSessionRepository()),
    )

    @Test
    fun `validate - de trong tieu de bao loi EMPTY_TITLE`() {
        val repo = FakeCreateHousingRepository()
        val viewModel = viewModel(repo)

        viewModel.onAction(CreateHousingPostAction.Submit)

        assertEquals(CreateHousingPostError.EMPTY_TITLE, viewModel.uiState.value.validationError)
        assertNull(repo.createdListing)
    }

    @Test
    fun `validate - de trong gia bao loi INVALID_RENT`() {
        val repo = FakeCreateHousingRepository()
        val viewModel = viewModel(repo)

        viewModel.onAction(CreateHousingPostAction.ChangeTitle("Phòng trọ đẹp"))
        viewModel.onAction(CreateHousingPostAction.Submit)

        assertEquals(CreateHousingPostError.INVALID_RENT, viewModel.uiState.value.validationError)
    }

    @Test
    fun `submit hop le - goi repository va phat event PostCreated`() = runTest {
        val repo = FakeCreateHousingRepository()
        val viewModel = viewModel(repo)

        viewModel.onAction(CreateHousingPostAction.ChangeTitle("Phòng gác lửng gần CS1"))
        viewModel.onAction(CreateHousingPostAction.ChangeRent("2500000"))
        viewModel.onAction(CreateHousingPostAction.ChangeAreaLabel("Khu vực Quang Trung, Gò Vấp"))
        viewModel.onAction(CreateHousingPostAction.ChangeExactAddress("138 Quang Trung, Gò Vấp"))
        viewModel.onAction(CreateHousingPostAction.ChangeContactPhone("0901234567"))
        viewModel.onAction(CreateHousingPostAction.ChangeCampus(UthCampus.CS1_QUANG_TRUNG))

        viewModel.onAction(CreateHousingPostAction.Submit)

        val event = viewModel.events.first()
        assertEquals(CreateHousingPostEvent.PostCreated, event)
        assertNotNull(repo.createdListing)
        assertEquals("Phòng gác lửng gần CS1", repo.createdListing?.title)
        assertEquals(2_500_000, repo.createdListing?.monthlyRentVnd)
        assertEquals("Khu vực Quang Trung, Gò Vấp", repo.createdListing?.addressLabel)
        assertEquals("0901234567", repo.createdPrivateDetails?.contactPhone)
        assertEquals("138 Quang Trung, Gò Vấp", repo.createdPrivateDetails?.exactAddress)
        assertEquals("2251001234", repo.createdListing?.hostStudentId)
        assertEquals("Sinh viên kiểm thử", repo.createdListing?.hostName)
        assertTrue(repo.createdListing?.verified == true)
    }

    @Test
    fun `allowZalo mac dinh true va ChangeAllowZalo toggle duoc`() {
        val viewModel = viewModel(FakeCreateHousingRepository())
        assertTrue(viewModel.uiState.value.allowZalo)

        viewModel.onAction(CreateHousingPostAction.ChangeAllowZalo(false))
        assertFalse(viewModel.uiState.value.allowZalo)

        viewModel.onAction(CreateHousingPostAction.ChangeAllowZalo(true))
        assertTrue(viewModel.uiState.value.allowZalo)
    }

    @Test
    fun `allowZalo tat - contactZaloUrl rong khi dang tin`() = runTest {
        val repo = FakeCreateHousingRepository()
        val viewModel = viewModel(repo)
        fillValidForm(viewModel)
        viewModel.onAction(CreateHousingPostAction.ChangeContactZalo("https://zalo.me/0901234567"))
        viewModel.onAction(CreateHousingPostAction.ChangeAllowZalo(false))

        viewModel.onAction(CreateHousingPostAction.Submit)

        assertEquals(CreateHousingPostEvent.PostCreated, viewModel.events.first())
        assertEquals("", repo.createdPrivateDetails?.contactZaloUrl)
    }

    @Test
    fun `allowZalo bat - chua nhap link thi fallback theo so dien thoai`() = runTest {
        val repo = FakeCreateHousingRepository()
        val viewModel = viewModel(repo)
        fillValidForm(viewModel)

        viewModel.onAction(CreateHousingPostAction.Submit)

        assertEquals(CreateHousingPostEvent.PostCreated, viewModel.events.first())
        assertEquals("https://zalo.me/0901234567", repo.createdPrivateDetails?.contactZaloUrl)
    }

    @Test
    fun `SaveDraft - phat DraftSaved giu nguyen form va khong dang tin`() = runTest {
        val repo = FakeCreateHousingRepository()
        val viewModel = viewModel(repo)
        viewModel.onAction(CreateHousingPostAction.ChangeTitle("Nháp phòng gác lửng"))

        viewModel.onAction(CreateHousingPostAction.SaveDraft)

        assertEquals(CreateHousingPostEvent.DraftSaved, viewModel.events.first())
        assertEquals("Nháp phòng gác lửng", viewModel.uiState.value.title)
        assertNull("luu nhap khong duoc tao tin that", repo.createdListing)
        assertFalse(viewModel.uiState.value.isSubmitting)
    }

    @Test
    fun `AddPhoto - khong vuot tran MAX_HOUSING_PHOTOS`() {
        val viewModel = viewModel(FakeCreateHousingRepository())

        repeat(MAX_HOUSING_PHOTOS + 1) { index ->
            viewModel.onAction(CreateHousingPostAction.AddPhoto("content://photo/$index"))
        }

        assertEquals(MAX_HOUSING_PHOTOS, viewModel.uiState.value.photos.size)
    }

    @Test
    fun `RemovePhoto - xoa dung thu tu anh`() {
        val viewModel = viewModel(FakeCreateHousingRepository())
        viewModel.onAction(CreateHousingPostAction.AddPhoto("content://photo/0"))
        viewModel.onAction(CreateHousingPostAction.AddPhoto("content://photo/1"))

        viewModel.onAction(CreateHousingPostAction.RemovePhoto(0))

        assertEquals(listOf("content://photo/1"), viewModel.uiState.value.photos)
    }

    private fun fillValidForm(viewModel: CreateHousingPostViewModel) {
        viewModel.onAction(CreateHousingPostAction.ChangeTitle("Phòng gác lửng gần CS1"))
        viewModel.onAction(CreateHousingPostAction.ChangeRent("2500000"))
        viewModel.onAction(CreateHousingPostAction.ChangeAreaLabel("Khu vực Quang Trung, Gò Vấp"))
        viewModel.onAction(CreateHousingPostAction.ChangeExactAddress("138 Quang Trung, Gò Vấp"))
        viewModel.onAction(CreateHousingPostAction.ChangeContactPhone("0901234567"))
    }
}
