package com.giathinh.uthconnect.presentation.housing.detail

import androidx.lifecycle.SavedStateHandle
import com.giathinh.uthconnect.domain.model.GenderPreference
import com.giathinh.uthconnect.domain.model.HousingContactRequest
import com.giathinh.uthconnect.domain.model.HousingContactStatus
import com.giathinh.uthconnect.domain.model.HousingListing
import com.giathinh.uthconnect.domain.model.HousingListingStatus
import com.giathinh.uthconnect.domain.model.HousingListingType
import com.giathinh.uthconnect.domain.model.HousingPrivateDetails
import com.giathinh.uthconnect.domain.model.ServerErrorCode
import com.giathinh.uthconnect.domain.model.UthCampus
import com.giathinh.uthconnect.domain.repository.HousingRepository
import com.giathinh.uthconnect.domain.usecase.GetHousingListingDetailUseCase
import com.giathinh.uthconnect.domain.usecase.MarkHousingListingFulfilledUseCase
import com.giathinh.uthconnect.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
        monthlyRentVnd = 2_500_000,
        depositVnd = 1_000_000,
        utilitiesIncluded = true,
        availableSlots = 1,
        genderPreference = GenderPreference.ANY,
        scheduleNote = "Giờ tự do",
        addressLabel = "Khu vực Quang Trung, Gò Vấp",
        approxLatitude = 10.83,
        approxLongitude = 106.65,
    )

    private val sampleDetails = HousingPrivateDetails(
        latitude = 10.8313,
        longitude = 106.6553,
        exactAddress = "138 Quang Trung, Gò Vấp",
        contactPhone = "0901234567",
        contactZaloUrl = "https://zalo.me/0901234567",
    )

    private fun request(status: HousingContactStatus) = HousingContactRequest(
        id = "RQ-1",
        listingId = "HS-001",
        requesterId = "SV-002",
        requesterName = "Người hỏi",
        status = status,
        message = "",
        createdAtMillis = 0L,
    )

    private fun createViewModel(
        repository: FakeHousingDetailRepository = FakeHousingDetailRepository(sampleListing),
        listingId: String = "HS-001",
    ): HousingDetailViewModel {
        val savedStateHandle = SavedStateHandle(mapOf("listingId" to listingId))
        return HousingDetailViewModel(
            savedStateHandle = savedStateHandle,
            getListingDetail = GetHousingListingDetailUseCase(repository),
            markFulfilled = MarkHousingListingFulfilledUseCase(repository),
            housing = repository,
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
    }

    @Test
    fun `chua mo khoa - khong co privateDetails va CallHost khong phat event`() = runTest {
        val viewModel = createViewModel()

        assertFalse(viewModel.uiState.value.isRevealed)
        viewModel.onAction(HousingDetailAction.CallHost)

        assertNull(withTimeoutOrNull(1_000) { viewModel.events.first() })
    }

    @Test
    fun `chua mo khoa - OpenZalo khong phat event`() = runTest {
        val viewModel = createViewModel()

        viewModel.onAction(HousingDetailAction.OpenZalo)

        assertNull(withTimeoutOrNull(1_000) { viewModel.events.first() })
    }

    @Test
    fun `request ACCEPTED - mo khoa va CallHost phat so private`() = runTest {
        val repository = FakeHousingDetailRepository(
            sampleListing,
            myRequest = request(HousingContactStatus.ACCEPTED),
            privateDetails = sampleDetails,
        )
        val viewModel = createViewModel(repository)

        assertTrue(viewModel.uiState.value.isRevealed)
        viewModel.onAction(HousingDetailAction.CallHost)

        val event = viewModel.events.first()
        assertTrue(event is HousingDetailEvent.CallPhone)
        assertEquals("0901234567", (event as HousingDetailEvent.CallPhone).phoneNumber)
    }

    @Test
    fun `request ACCEPTED - OpenZalo phat link private`() = runTest {
        val repository = FakeHousingDetailRepository(
            sampleListing,
            myRequest = request(HousingContactStatus.ACCEPTED),
            privateDetails = sampleDetails,
        )
        val viewModel = createViewModel(repository)

        viewModel.onAction(HousingDetailAction.OpenZalo)

        val event = viewModel.events.first()
        assertTrue(event is HousingDetailEvent.OpenZalo)
        assertEquals("https://zalo.me/0901234567", (event as HousingDetailEvent.OpenZalo).url)
    }

    @Test
    fun `OpenMap - chua mo khoa dung toa do xap xi va khu vuc`() = runTest {
        val viewModel = createViewModel()

        viewModel.onAction(HousingDetailAction.OpenMap)

        val event = viewModel.events.first() as HousingDetailEvent.OpenMap
        assertEquals(10.83, event.latitude, 0.0)
        assertEquals(106.65, event.longitude, 0.0)
        assertEquals("Khu vực Quang Trung, Gò Vấp", event.addressLabel)
    }

    @Test
    fun `OpenMap - da mo khoa dung toa do that va dia chi day du`() = runTest {
        val repository = FakeHousingDetailRepository(
            sampleListing,
            myRequest = request(HousingContactStatus.ACCEPTED),
            privateDetails = sampleDetails,
        )
        val viewModel = createViewModel(repository)

        viewModel.onAction(HousingDetailAction.OpenMap)

        val event = viewModel.events.first() as HousingDetailEvent.OpenMap
        assertEquals(10.8313, event.latitude, 0.0)
        assertEquals(106.6553, event.longitude, 0.0)
        assertEquals("138 Quang Trung, Gò Vấp", event.addressLabel)
    }

    @Test
    fun `chu tin - nap danh sach request va tu mo khoa`() {
        val repository = FakeHousingDetailRepository(
            sampleListing.copy(isOwner = true),
            requests = listOf(request(HousingContactStatus.PENDING)),
            privateDetails = sampleDetails,
        )
        val viewModel = createViewModel(repository)

        val state = viewModel.uiState.value
        assertTrue(state.isOwner)
        assertEquals(1, state.contactRequests.size)
        assertTrue("chủ tin luôn thấy thông tin private", state.isRevealed)
    }

    @Test
    fun `RequestContact - gui thanh cong phat ContactRequestSent va trang thai PENDING`() = runTest {
        val repository = FakeHousingDetailRepository(sampleListing)
        val viewModel = createViewModel(repository)

        viewModel.onAction(HousingDetailAction.RequestContact)

        assertEquals(HousingDetailEvent.ContactRequestSent, viewModel.events.first())
        assertEquals(listOf("HS-001"), repository.requestCalls)
        assertEquals(HousingContactStatus.PENDING, viewModel.uiState.value.myRequest?.status)
        assertFalse(viewModel.uiState.value.isActionLoading)
    }

    @Test
    fun `RequestContact that bai - phat ContactActionFailed dung ma loi`() = runTest {
        val repository = FakeHousingDetailRepository(
            sampleListing,
            requestFailure = IllegalStateException("HOUSING_CONTACT_SELF_REQUEST"),
        )
        val viewModel = createViewModel(repository)

        viewModel.onAction(HousingDetailAction.RequestContact)

        val event = viewModel.events.first()
        assertTrue(event is HousingDetailEvent.ContactActionFailed)
        assertEquals(ServerErrorCode.HousingContactSelfRequest, (event as HousingDetailEvent.ContactActionFailed).code)
        assertFalse(viewModel.uiState.value.isActionLoading)
    }

    @Test
    fun `CancelContact - huy request PENDING cua minh`() = runTest {
        val repository = FakeHousingDetailRepository(sampleListing, myRequest = request(HousingContactStatus.PENDING))
        val viewModel = createViewModel(repository)

        viewModel.onAction(HousingDetailAction.CancelContact)

        assertEquals(listOf("RQ-1"), repository.cancelCalls)
        assertEquals(HousingContactStatus.CANCELLED, viewModel.uiState.value.myRequest?.status)
    }

    @Test
    fun `RespondContact - chu tin chap nhan request`() = runTest {
        val repository = FakeHousingDetailRepository(
            sampleListing.copy(isOwner = true),
            requests = listOf(request(HousingContactStatus.PENDING)),
            privateDetails = sampleDetails,
        )
        val viewModel = createViewModel(repository)

        viewModel.onAction(HousingDetailAction.RespondContact("RQ-1", accept = true))

        assertEquals(listOf("RQ-1" to true), repository.respondCalls)
        assertEquals(HousingContactStatus.ACCEPTED, viewModel.uiState.value.contactRequests.first().status)
    }

    @Test
    fun `danh dau het phong - cap nhat status thanh cong`() = runTest {
        val repository = FakeHousingDetailRepository(sampleListing.copy(isOwner = true), privateDetails = sampleDetails)
        val viewModel = createViewModel(repository)

        viewModel.onAction(HousingDetailAction.MarkFulfilled)

        val event = viewModel.events.first()
        assertTrue(event is HousingDetailEvent.MarkFulfilledSuccess)
        assertEquals(HousingListingStatus.FULFILLED, repository.listing.status)
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

    private class FakeHousingDetailRepository(
        var listing: HousingListing,
        var loadFailure: Throwable? = null,
        var myRequest: HousingContactRequest? = null,
        var requests: List<HousingContactRequest> = emptyList(),
        var privateDetails: HousingPrivateDetails? = null,
        var requestFailure: Throwable? = null,
    ) : HousingRepository {
        val requestCalls = mutableListOf<String>()
        val respondCalls = mutableListOf<Pair<String, Boolean>>()
        val cancelCalls = mutableListOf<String>()

        override suspend fun getListings(): Result<List<HousingListing>> = Result.success(listOf(listing))

        override suspend fun getListingById(id: String): Result<HousingListing> {
            val error = loadFailure
            return if (error != null) Result.failure(error) else Result.success(listing)
        }

        override suspend fun createListing(
            listing: HousingListing,
            privateDetails: HousingPrivateDetails,
        ): Result<HousingListing> = Result.success(listing)

        override suspend fun updateListing(
            listing: HousingListing,
            privateDetails: HousingPrivateDetails,
        ): Result<HousingListing> = Result.success(listing)

        override suspend fun markListingFulfilled(id: String): Result<Unit> {
            listing = listing.copy(status = HousingListingStatus.FULFILLED, availableSlots = 0)
            return Result.success(Unit)
        }

        override suspend fun deleteListing(id: String): Result<Unit> = Result.success(Unit)

        override suspend fun reportListing(listingId: String, reason: String): Result<Unit> = Result.success(Unit)

        override suspend fun requestContact(listingId: String, message: String): Result<String> {
            requestCalls += listingId
            val error = requestFailure
            if (error != null) return Result.failure(error)
            myRequest = HousingContactRequest(
                id = "RQ-1",
                listingId = listingId,
                requesterId = "SV-002",
                requesterName = "Người hỏi",
                status = HousingContactStatus.PENDING,
                message = message,
                createdAtMillis = 0L,
            )
            return Result.success("RQ-1")
        }

        override suspend fun respondContact(requestId: String, accept: Boolean): Result<Unit> {
            respondCalls += requestId to accept
            val status = if (accept) HousingContactStatus.ACCEPTED else HousingContactStatus.REJECTED
            requests = requests.map { if (it.id == requestId) it.copy(status = status) else it }
            if (myRequest?.id == requestId) myRequest = myRequest?.copy(status = status)
            return Result.success(Unit)
        }

        override suspend fun cancelContact(requestId: String): Result<Unit> {
            cancelCalls += requestId
            if (myRequest?.id == requestId) myRequest = myRequest?.copy(status = HousingContactStatus.CANCELLED)
            return Result.success(Unit)
        }

        override suspend fun getContactRequests(listingId: String): Result<List<HousingContactRequest>> = Result.success(requests)

        override suspend fun getMyContactRequest(listingId: String): Result<HousingContactRequest?> = Result.success(myRequest)

        override suspend fun revealPrivateDetails(listingId: String): Result<HousingPrivateDetails> {
            val details = privateDetails
            return if (details != null) Result.success(details) else Result.failure(NoSuchElementException(listingId))
        }
    }
}
