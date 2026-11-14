package com.giathinh.uthconnect.presentation.housing.detail

import androidx.lifecycle.SavedStateHandle
import com.giathinh.uthconnect.domain.model.GenderPreference
import com.giathinh.uthconnect.domain.model.HousingListing
import com.giathinh.uthconnect.domain.model.HousingListingStatus
import com.giathinh.uthconnect.domain.model.HousingListingType
import com.giathinh.uthconnect.domain.model.StudentProfile
import com.giathinh.uthconnect.domain.model.UthCampus
import com.giathinh.uthconnect.domain.repository.HousingRepository
import com.giathinh.uthconnect.domain.repository.SessionRepository
import com.giathinh.uthconnect.domain.repository.StudentSession
import com.giathinh.uthconnect.domain.usecase.AccessStudentSessionUseCase
import com.giathinh.uthconnect.domain.usecase.DeleteHousingListingUseCase
import com.giathinh.uthconnect.domain.usecase.GetHousingListingDetailUseCase
import com.giathinh.uthconnect.domain.usecase.MarkHousingListingFulfilledUseCase
import com.giathinh.uthconnect.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HousingDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val sampleListing = HousingListing(
        id = "HS-001",
        type = HousingListingType.ROOM_FOR_RENT,
        campus = UthCampus.CS1_QUANG_TRUNG,
        title = "Phòng trọ test",
        description = "Mô tả test",
        hostName = "Nguyễn Văn A",
        hostStudentId = "2251010001",
        contactPhone = "0901234567",
        contactZaloUrl = "https://zalo.me/0901234567",
        monthlyRentVnd = 2_500_000,
        depositVnd = 1_000_000,
        utilitiesIncluded = true,
        availableSlots = 1,
        genderPreference = GenderPreference.ANY,
        scheduleNote = "Giờ tự do",
        addressLabel = "Quang Trung, Gò Vấp",
        latitude = 10.83,
        longitude = 106.65,
    )

    private fun createViewModel(
        repository: FakeHousingDetailRepository = FakeHousingDetailRepository(sampleListing),
        listingId: String = "HS-001",
        sessionStudentId: String? = "2251010001",
    ): HousingDetailViewModel {
        val savedStateHandle = SavedStateHandle(mapOf("listingId" to listingId))
        return HousingDetailViewModel(
            savedStateHandle = savedStateHandle,
            getListingDetail = GetHousingListingDetailUseCase(repository),
            markFulfilled = MarkHousingListingFulfilledUseCase(repository),
            deleteListing = DeleteHousingListingUseCase(repository),
            sessions = AccessStudentSessionUseCase(FakeSessionRepository(sessionStudentId)),
        )
    }

    @Test
    fun `mo man chi tiet - nap du lieu thanh cong`() {
        val viewModel = createViewModel()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNotNull(state.listing)
        assertEquals("HS-001", state.listing?.id)
        assertEquals("2251010001", state.listing?.hostStudentId)
        assertEquals("0901234567", state.listing?.contactPhone)
    }

    @Test
    fun `goi dien chu tin - phat event CallPhone`() = runTest {
        val viewModel = createViewModel()

        viewModel.onAction(HousingDetailAction.CallHost)

        val event = viewModel.events.first()
        assertTrue(event is HousingDetailEvent.CallPhone)
        assertEquals("0901234567", (event as HousingDetailEvent.CallPhone).phoneNumber)
    }

    @Test
    fun `mo zalo chu tin - phat event OpenZalo`() = runTest {
        val viewModel = createViewModel()

        viewModel.onAction(HousingDetailAction.OpenZalo)

        val event = viewModel.events.first()
        assertTrue(event is HousingDetailEvent.OpenZalo)
        assertEquals("https://zalo.me/0901234567", (event as HousingDetailEvent.OpenZalo).url)
    }

    @Test
    fun `zalo doi cu ngoai mien - bao InvalidZaloLink chu khong mo`() = runTest {
        val repository = FakeHousingDetailRepository(sampleListing.copy(contactZaloUrl = "https://evil.example/abc"))
        val viewModel = createViewModel(repository)

        viewModel.onAction(HousingDetailAction.OpenZalo)

        assertEquals(HousingDetailEvent.InvalidZaloLink, viewModel.events.first())
    }

    @Test
    fun `zalo doi cu dung http - bao InvalidZaloLink`() = runTest {
        val repository = FakeHousingDetailRepository(sampleListing.copy(contactZaloUrl = "http://zalo.me/0901234567"))
        val viewModel = createViewModel(repository)

        viewModel.onAction(HousingDetailAction.OpenZalo)

        assertEquals(HousingDetailEvent.InvalidZaloLink, viewModel.events.first())
    }

    @Test
    fun `zalo trong - fallback sdt thanh link zalo me`() = runTest {
        val repository = FakeHousingDetailRepository(sampleListing.copy(contactZaloUrl = ""))
        val viewModel = createViewModel(repository)

        viewModel.onAction(HousingDetailAction.OpenZalo)

        val event = viewModel.events.first()
        assertTrue(event is HousingDetailEvent.OpenZalo)
        assertEquals("https://zalo.me/0901234567", (event as HousingDetailEvent.OpenZalo).url)
    }

    @Test
    fun `zalo bi chu tin tat - khong mo ke ca fallback sdt`() = runTest {
        val repository = FakeHousingDetailRepository(
            sampleListing.copy(contactZaloUrl = "", contactZaloEnabled = false),
        )
        val viewModel = createViewModel(repository)

        viewModel.onAction(HousingDetailAction.OpenZalo)

        assertNull("chu tin tat Zalo thi khong duoc phat event", withTimeoutOrNull(1_000) { viewModel.events.first() })
    }

    @Test
    fun `danh dau het phong - cap nhat status thanh cong`() = runTest {
        val repository = FakeHousingDetailRepository(sampleListing)
        val viewModel = createViewModel(repository)

        viewModel.onAction(HousingDetailAction.MarkFulfilled)

        val event = viewModel.events.first()
        assertTrue(event is HousingDetailEvent.MarkFulfilledSuccess)
        assertEquals(HousingListingStatus.FULFILLED, repository.listing.status)
    }

    @Test
    fun `danh dau het phong thanh cong - nut thao tac mo lai sau khi tai lai`() = runTest {
        val repository = FakeHousingDetailRepository(sampleListing)
        val viewModel = createViewModel(repository)

        viewModel.onAction(HousingDetailAction.MarkFulfilled)

        val event = viewModel.events.first()
        assertTrue(event is HousingDetailEvent.MarkFulfilledSuccess)
        assertFalse(viewModel.uiState.value.isActionLoading)
        assertEquals(HousingListingStatus.FULFILLED, viewModel.uiState.value.listing?.status)
    }

    @Test
    fun `danh dau het phong - tai lai that bai van mo nut thao tac`() = runTest {
        val repository = FakeHousingDetailRepository(sampleListing)
        val viewModel = createViewModel(repository)
        repository.loadFailure = IllegalStateException("mat mang")

        viewModel.onAction(HousingDetailAction.MarkFulfilled)

        val event = viewModel.events.first()
        assertTrue(event is HousingDetailEvent.MarkFulfilledSuccess)
        assertFalse(viewModel.uiState.value.isActionLoading)
        assertEquals(HousingDetailFailure.LoadFailed, viewModel.uiState.value.failure)
        assertNull("tai lai that bai phai xoa tin cu de khong ho thanh lien he", viewModel.uiState.value.listing)
        assertFalse(viewModel.uiState.value.isOwner)
    }

    @Test
    fun `OpenMap - phat toa do that va dia chi de Route dung deep-link`() = runTest {
        val viewModel = createViewModel()

        viewModel.onAction(HousingDetailAction.OpenMap)

        val event = viewModel.events.first()
        assertTrue(event is HousingDetailEvent.OpenMap)
        val map = event as HousingDetailEvent.OpenMap
        assertEquals(10.83, map.latitude, 0.0)
        assertEquals(106.65, map.longitude, 0.0)
        assertEquals("Quang Trung, Gò Vấp", map.addressLabel)
    }

    @Test
    fun `OpenMap - tin chua gan vi tri thi gui toa do 0 kem dia chi`() = runTest {
        val repository = FakeHousingDetailRepository(sampleListing.copy(latitude = 0.0, longitude = 0.0))
        val viewModel = createViewModel(repository)

        viewModel.onAction(HousingDetailAction.OpenMap)

        val event = viewModel.events.first() as HousingDetailEvent.OpenMap
        assertEquals(0.0, event.latitude, 0.0)
        assertEquals(0.0, event.longitude, 0.0)
        assertEquals("Quang Trung, Gò Vấp", event.addressLabel)
    }

    @Test
    fun `khong tim thay tin - failure NotFound chu khong phai LoadFailed`() {
        val repository = FakeHousingDetailRepository(sampleListing, loadFailure = NoSuchElementException("HS-404"))
        val viewModel = createViewModel(repository, listingId = "HS-404")

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(HousingDetailFailure.NotFound, state.failure)
        assertNull(state.listing)
    }

    @Test
    fun `loi mang - failure LoadFailed va Retry tai lai thanh cong`() {
        val repository = FakeHousingDetailRepository(sampleListing, loadFailure = IllegalStateException("mat mang"))
        val viewModel = createViewModel(repository)
        assertEquals(HousingDetailFailure.LoadFailed, viewModel.uiState.value.failure)

        repository.loadFailure = null
        viewModel.onAction(HousingDetailAction.Retry)

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.failure)
        assertEquals("HS-001", state.listing?.id)
    }

    @Test
    fun `chu tin - MSSV trung phien dang nhap thi isOwner true`() {
        val viewModel = createViewModel()

        assertTrue(viewModel.uiState.value.isOwner)
    }

    @Test
    fun `nguoi xem - MSSV khac thi isOwner false`() {
        val viewModel = createViewModel(sessionStudentId = "2251099999")

        assertFalse(viewModel.uiState.value.isOwner)
    }

    @Test
    fun `tin khong co MSSV chu - isOwner false du dang nhap`() {
        val repository = FakeHousingDetailRepository(sampleListing.copy(hostStudentId = ""))
        val viewModel = createViewModel(repository)

        assertFalse(viewModel.uiState.value.isOwner)
    }

    @Test
    fun `chua dang nhap - isOwner false`() {
        val viewModel = createViewModel(sessionStudentId = null)

        assertFalse(viewModel.uiState.value.isOwner)
    }

    @Test
    fun `xoa tin - phat event Deleted va goi dung id`() = runTest {
        val repository = FakeHousingDetailRepository(sampleListing)
        val viewModel = createViewModel(repository)

        viewModel.onAction(HousingDetailAction.Delete)

        val event = viewModel.events.first()
        assertTrue(event is HousingDetailEvent.Deleted)
        assertEquals("HS-001", repository.deletedId)
    }

    @Test
    fun `xoa tin that bai - ActionFailed va nut mo lai`() = runTest {
        val repository = FakeHousingDetailRepository(sampleListing, deleteFailure = IllegalStateException("mat mang"))
        val viewModel = createViewModel(repository)

        viewModel.onAction(HousingDetailAction.Delete)

        val event = viewModel.events.first()
        assertTrue(event is HousingDetailEvent.ActionFailed)
        assertFalse(viewModel.uiState.value.isActionLoading)
        assertNull(repository.deletedId)
    }

    @Test
    fun `chu tin - tin REJECTED hien banner tu choi`() {
        val state = HousingDetailUiState(
            isLoading = false,
            listing = sampleListing.copy(status = HousingListingStatus.REJECTED, rejectionReason = "Ảnh mờ"),
            isOwner = true,
        )
        assertTrue(state.showRejectionBanner)
    }

    @Test
    fun `khong phai chu tin - khong hien banner tu choi`() {
        val state = HousingDetailUiState(
            isLoading = false,
            listing = sampleListing.copy(status = HousingListingStatus.REJECTED, rejectionReason = "Ảnh mờ"),
            isOwner = false,
        )
        assertFalse(state.showRejectionBanner)
    }

    @Test
    fun `tin PENDING - khong hien banner tu choi`() {
        val state = HousingDetailUiState(
            isLoading = false,
            listing = sampleListing.copy(status = HousingListingStatus.PENDING),
            isOwner = true,
        )
        assertFalse(state.showRejectionBanner)
    }

    @Test
    fun `tin OPEN - khong hien banner tu choi`() {
        val state = HousingDetailUiState(
            isLoading = false,
            listing = sampleListing.copy(status = HousingListingStatus.OPEN),
            isOwner = true,
        )
        assertFalse(state.showRejectionBanner)
    }

    private class FakeSessionRepository(studentId: String?) : SessionRepository {
        override val session: StateFlow<StudentSession> = MutableStateFlow(
            StudentSession(
                isRestoring = false,
                profile = studentId?.let {
                    StudentProfile(
                        studentId = it,
                        email = "student@ut.edu.vn",
                        displayName = "Sinh viên kiểm thử",
                        className = "22DTH",
                        major = "CNTT",
                        faculty = "Công nghệ thông tin",
                        uthVerified = true,
                    )
                },
            ),
        )

        override suspend fun restore() = Unit
        override suspend fun start(profile: StudentProfile, remember: Boolean) = Unit
        override suspend fun updateProfile(profile: StudentProfile) = Unit
        override suspend fun enableBiometric(studentId: String) = Unit
        override suspend fun disableBiometric() = Unit
        override suspend fun signOut() = Unit
    }

    private class FakeHousingDetailRepository(
        var listing: HousingListing,
        var loadFailure: Throwable? = null,
        var deleteFailure: Throwable? = null,
    ) : HousingRepository {
        var deletedId: String? = null

        override suspend fun getListings(): Result<List<HousingListing>> = Result.success(listOf(listing))

        override suspend fun getMyListings(): Result<List<HousingListing>> = Result.success(listOf(listing))

        override suspend fun getListingById(id: String): Result<HousingListing> {
            val error = loadFailure
            return if (error != null) Result.failure(error) else Result.success(listing)
        }

        override suspend fun createListing(listing: HousingListing): Result<HousingListing> = Result.success(listing)

        override suspend fun updateListing(listing: HousingListing): Result<HousingListing> = Result.success(listing)

        override suspend fun markListingFulfilled(id: String): Result<Unit> {
            listing = listing.copy(status = HousingListingStatus.FULFILLED, availableSlots = 0)
            return Result.success(Unit)
        }

        override suspend fun deleteListing(id: String): Result<Unit> {
            val error = deleteFailure
            if (error != null) return Result.failure(error)
            deletedId = id
            return Result.success(Unit)
        }

        override suspend fun reportListing(listingId: String, reason: String): Result<Unit> = Result.success(Unit)
    }
}
