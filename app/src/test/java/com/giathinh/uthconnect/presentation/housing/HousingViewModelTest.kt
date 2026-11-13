package com.giathinh.uthconnect.presentation.housing

import com.giathinh.uthconnect.domain.model.GenderPreference
import com.giathinh.uthconnect.domain.model.HousingListing
import com.giathinh.uthconnect.domain.model.HousingListingType
import com.giathinh.uthconnect.domain.model.UthCampus
import com.giathinh.uthconnect.domain.repository.HousingRepository
import com.giathinh.uthconnect.domain.usecase.GetHousingListingsUseCase
import com.giathinh.uthconnect.domain.usecase.ReportHousingListingUseCase
import com.giathinh.uthconnect.testing.MainDispatcherRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HousingViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val listings = listOf(
        HousingListing(
            id = "HS-1",
            type = HousingListingType.ROOM_FOR_RENT,
            campus = UthCampus.CS1_QUANG_TRUNG,
            title = "Phòng gần CS1",
            description = "",
            hostName = "Chủ tin",
            monthlyRentVnd = 2_000_000,
            depositVnd = 0,
            utilitiesIncluded = true,
            availableSlots = 1,
            genderPreference = GenderPreference.ANY,
            scheduleNote = "",
            addressLabel = "Quang Trung",
            contactPhone = "0901234567",
            latitude = 10.8313,
            longitude = 106.6553,
            distanceToCampusM = 300,
        ),
    )

    private fun createViewModel(repository: FakeHousingRepository = FakeHousingRepository(listings)): HousingViewModel = HousingViewModel(
        getListings = GetHousingListingsUseCase(repository),
        reportListing = ReportHousingListingUseCase(repository),
    )

    @Test
    fun `mo man - feed nap tu repository va loading tat di`() {
        val viewModel = createViewModel()

        val state = viewModel.uiState.value
        assertEquals(listings, state.listings)
        assertFalse(state.isLoading)
        assertNull(state.failure)
    }

    @Test
    fun `repository that bai - UiState bao LoadFailed va danh sach rong`() {
        val viewModel = createViewModel(FakeHousingRepository(listings, failLoad = true))

        val state = viewModel.uiState.value
        assertEquals(HousingFailure.LoadFailed, state.failure)
        assertTrue(state.listings.isEmpty())
        assertFalse(state.isLoading)
    }

    @Test
    fun `action Retry - nap lai va xoa failure`() {
        val repository = FakeHousingRepository(listings, failLoad = true)
        val viewModel = createViewModel(repository)
        assertEquals(HousingFailure.LoadFailed, viewModel.uiState.value.failure)

        repository.failLoad = false
        viewModel.onAction(HousingAction.Retry)

        val state = viewModel.uiState.value
        assertEquals(listings, state.listings)
        assertNull(state.failure)
    }

    @Test
    fun `UiState mac dinh la loading khong phai empty`() {
        val initial = HousingUiState()

        assertTrue(initial.isLoading)
        assertFalse("dang tai thi chua duoc coi la rong", initial.isEmpty)
    }

    @Test
    fun `OpenReport - mo dialog voi ly do chua chon`() {
        val viewModel = createViewModel()

        viewModel.onAction(HousingAction.OpenReport("HS-1"))

        val state = viewModel.uiState.value
        assertTrue(state.isReportDialogOpen)
        assertEquals("HS-1", state.reportingListingId)
        assertNull("ly do phai null cho toi khi nguoi dung chon", state.reportingReason)
    }

    @Test
    fun `ConfirmReport khi chua chon ly do - khong goi repository`() = runTest {
        val repository = FakeHousingRepository(listings)
        val viewModel = createViewModel(repository)
        viewModel.onAction(HousingAction.OpenReport("HS-1"))

        viewModel.onAction(HousingAction.ConfirmReport)

        assertTrue("chua chon ly do thi khong duoc gui", repository.reportCalls.isEmpty())
        assertNull(withTimeoutOrNull(1_000) { viewModel.events.first() })
    }

    @Test
    fun `ConfirmReport hop le - gui dung ly do dong dialog va phat ReportAccepted`() = runTest {
        val repository = FakeHousingRepository(listings)
        val viewModel = createViewModel(repository)
        viewModel.onAction(HousingAction.OpenReport("HS-1"))
        viewModel.onAction(HousingAction.SelectReportReason(HousingReportReason.FAKE_INFO))

        viewModel.onAction(HousingAction.ConfirmReport)

        assertEquals(listOf("HS-1" to "FAKE_INFO"), repository.reportCalls)
        assertFalse(viewModel.uiState.value.isReportDialogOpen)
        assertNull(viewModel.uiState.value.reportingReason)
        assertEquals(HousingEvent.ReportAccepted, viewModel.events.first())
    }

    @Test
    fun `report that bai - giu dialog mo va phat ReportRejected`() = runTest {
        val repository = FakeHousingRepository(listings, failReport = true)
        val viewModel = createViewModel(repository)
        viewModel.onAction(HousingAction.OpenReport("HS-1"))
        viewModel.onAction(HousingAction.SelectReportReason(HousingReportReason.WRONG_PRICE))

        viewModel.onAction(HousingAction.ConfirmReport)

        assertTrue("gui that bai thi dialog giu nguyen de gui lai", viewModel.uiState.value.isReportDialogOpen)
        assertNull("loi bao tin khong chan feed", viewModel.uiState.value.failure)
        assertEquals(HousingEvent.ReportRejected, viewModel.events.first())
    }

    @Test
    fun `ClearFilters - reset moi bo loc va sort ve mac dinh`() {
        val viewModel = createViewModel()
        viewModel.onAction(HousingAction.SelectType(HousingListingType.ROOM_FOR_RENT))
        viewModel.onAction(HousingAction.SelectCampus(UthCampus.CS2_THU_DUC))
        viewModel.onAction(HousingAction.SelectMaxRent(2_000_000))
        viewModel.onAction(HousingAction.SelectMaxDistance(1_000))
        viewModel.onAction(HousingAction.SelectGender(GenderPreference.FEMALE))
        viewModel.onAction(HousingAction.SelectSort(HousingSort.CHEAPEST))
        assertTrue(viewModel.uiState.value.hasActiveFilters)

        viewModel.onAction(HousingAction.ClearFilters)

        val state = viewModel.uiState.value
        assertFalse(state.hasActiveFilters)
        assertEquals(HousingSort.NEWEST, state.sort)
    }

    @Test
    fun `OpenFilters va CloseFilters - dong mo dialog loc`() {
        val viewModel = createViewModel()

        viewModel.onAction(HousingAction.OpenFilters)
        assertTrue(viewModel.uiState.value.isFilterDialogOpen)

        viewModel.onAction(HousingAction.CloseFilters)
        assertFalse(viewModel.uiState.value.isFilterDialogOpen)
    }

    @Test
    fun `Search - luu tu khoa va loc feed theo tieu de hoac dia chi`() {
        val viewModel = createViewModel()

        viewModel.onAction(HousingAction.Search("QUANG TRUNG"))

        val state = viewModel.uiState.value
        assertEquals("QUANG TRUNG", state.searchQuery)
        assertEquals(listOf("HS-1"), state.filteredListings.map { it.id })
        assertTrue(state.hasActiveFilters)

        viewModel.onAction(HousingAction.Search("không tồn tại"))
        assertTrue(viewModel.uiState.value.filteredListings.isEmpty())
        assertTrue(viewModel.uiState.value.isEmptyBecauseOfFilters)
    }

    @Test
    fun `ClearFilters - xoa ca tu khoa tim kiem`() {
        val viewModel = createViewModel()
        viewModel.onAction(HousingAction.Search("gác lửng"))
        assertTrue(viewModel.uiState.value.hasActiveFilters)

        viewModel.onAction(HousingAction.ClearFilters)

        val state = viewModel.uiState.value
        assertEquals("", state.searchQuery)
        assertFalse(state.hasActiveFilters)
        assertEquals(listOf("HS-1"), state.filteredListings.map { it.id })
    }

    @Test
    fun `ToggleSaved - them roi bo tin khoi danh sach luu trong phien`() {
        val viewModel = createViewModel()
        assertTrue(viewModel.uiState.value.savedListingIds.isEmpty())

        viewModel.onAction(HousingAction.ToggleSaved("HS-1"))
        assertEquals(setOf("HS-1"), viewModel.uiState.value.savedListingIds)

        viewModel.onAction(HousingAction.ToggleSaved("HS-1"))
        assertTrue(viewModel.uiState.value.savedListingIds.isEmpty())
    }

    @Test
    fun `ToggleSaved - luu tin nay khong anh huong tin khac`() {
        val viewModel = createViewModel()

        viewModel.onAction(HousingAction.ToggleSaved("HS-1"))
        viewModel.onAction(HousingAction.ToggleSaved("HS-2"))
        viewModel.onAction(HousingAction.ToggleSaved("HS-1"))

        assertEquals(setOf("HS-2"), viewModel.uiState.value.savedListingIds)
    }

    @Test
    fun `CallHost - phat CallPhone voi so cua tin trong feed`() = runTest {
        val viewModel = createViewModel()

        viewModel.onAction(HousingAction.CallHost("HS-1"))

        val event = viewModel.events.first()
        assertTrue(event is HousingEvent.CallPhone)
        assertEquals("0901234567", (event as HousingEvent.CallPhone).phoneNumber)
    }

    @Test
    fun `CallHost - tin khong ton tai hoac thieu so thi khong phat event`() = runTest {
        val viewModel = createViewModel()

        viewModel.onAction(HousingAction.CallHost("HS-khong-ton-tai"))

        assertNull(withTimeoutOrNull(1_000) { viewModel.events.first() })
    }

    @Test
    fun `Refresh that bai khi da co feed - giu listings va bao RefreshFailed rieng`() = runTest {
        val repository = FakeHousingRepository(listings)
        val viewModel = createViewModel(repository)
        repository.failLoad = true

        viewModel.onAction(HousingAction.Refresh)

        val state = viewModel.uiState.value
        assertEquals("loi tai lai khong duoc thay feed bang man hinh loi", listings, state.listings)
        assertNull(state.failure)
        assertFalse(state.isLoading)
        assertEquals(HousingEvent.RefreshFailed, viewModel.events.first())
    }

    @Test
    fun `Refresh that bai khi chua co du lieu - van bao LoadFailed`() {
        val viewModel = createViewModel(FakeHousingRepository(listings, failLoad = true))

        viewModel.onAction(HousingAction.Refresh)

        assertEquals(HousingFailure.LoadFailed, viewModel.uiState.value.failure)
        assertTrue(viewModel.uiState.value.listings.isEmpty())
    }

    @Test
    fun `Refresh - ket qua tai cu khong ghi de ket qua moi hon`() = runTest {
        val repository = FakeHousingRepository(listings)
        val gate = CompletableDeferred<Unit>()
        repository.loadGate = gate
        val viewModel = createViewModel(repository)

        repository.listings = emptyList()
        repository.loadGate = null
        viewModel.onAction(HousingAction.Refresh)
        assertTrue(viewModel.uiState.value.listings.isEmpty())

        gate.complete(Unit)
        advanceUntilIdle()

        assertTrue("ket qua cu da bi huy khong duoc ghi de feed moi", viewModel.uiState.value.listings.isEmpty())
    }

    @Test
    fun `ConfirmReport gui lap khi dang cho - chi mot request`() = runTest {
        val repository = FakeHousingRepository(listings)
        val gate = CompletableDeferred<Unit>()
        repository.reportGate = gate
        val viewModel = createViewModel(repository)
        viewModel.onAction(HousingAction.OpenReport("HS-1"))
        viewModel.onAction(HousingAction.SelectReportReason(HousingReportReason.FAKE_INFO))

        viewModel.onAction(HousingAction.ConfirmReport)
        viewModel.onAction(HousingAction.ConfirmReport)
        gate.complete(Unit)

        assertEquals(1, repository.reportCalls.size)
        assertEquals(HousingEvent.ReportAccepted, viewModel.events.first())
    }

    @Test
    fun `bao tin cu hoan tat sau khi mo dialog tin khac - khong dong dialog moi`() = runTest {
        val repository = FakeHousingRepository(listings)
        val gate = CompletableDeferred<Unit>()
        repository.reportGate = gate
        val viewModel = createViewModel(repository)
        viewModel.onAction(HousingAction.OpenReport("HS-1"))
        viewModel.onAction(HousingAction.SelectReportReason(HousingReportReason.FAKE_INFO))
        viewModel.onAction(HousingAction.ConfirmReport)

        viewModel.onAction(HousingAction.OpenReport("HS-2"))
        viewModel.onAction(HousingAction.SelectReportReason(HousingReportReason.WRONG_PRICE))
        gate.complete(Unit)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("ket qua phien cu khong duoc dong dialog phien moi", "HS-2", state.reportingListingId)
        assertEquals(HousingReportReason.WRONG_PRICE, state.reportingReason)
        assertEquals(HousingEvent.ReportAccepted, viewModel.events.first())
    }

    private class FakeHousingRepository(
        var listings: List<HousingListing>,
        var failLoad: Boolean = false,
        var failReport: Boolean = false,
    ) : HousingRepository {
        val reportCalls = mutableListOf<Pair<String, String>>()
        var loadGate: CompletableDeferred<Unit>? = null
        var reportGate: CompletableDeferred<Unit>? = null

        override suspend fun getListings(): Result<List<HousingListing>> {
            // Chụp dữ liệu TRƯỚC khi chờ gate: kết quả của lần tải phải là dữ liệu
            // tại thời điểm gọi, nếu không test race không phân biệt được cũ/mới.
            val snapshot = listings
            loadGate?.await()
            return if (failLoad) Result.failure(IllegalStateException("mat mang")) else Result.success(snapshot)
        }

        override suspend fun getListingById(id: String): Result<HousingListing> {
            val item = listings.find { it.id == id }
            return if (item != null) Result.success(item) else Result.failure(NoSuchElementException(id))
        }

        override suspend fun createListing(listing: HousingListing): Result<HousingListing> = Result.success(listing)

        override suspend fun updateListing(listing: HousingListing): Result<HousingListing> = Result.success(listing)

        override suspend fun markListingFulfilled(id: String): Result<Unit> = Result.success(Unit)

        override suspend fun deleteListing(id: String): Result<Unit> = Result.success(Unit)

        override suspend fun reportListing(listingId: String, reason: String): Result<Unit> {
            reportCalls += listingId to reason
            reportGate?.await()
            return if (failReport) Result.failure(IllegalStateException("mat mang")) else Result.success(Unit)
        }
    }
}
