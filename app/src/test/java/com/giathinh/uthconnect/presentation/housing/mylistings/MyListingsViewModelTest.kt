package com.giathinh.uthconnect.presentation.housing.mylistings

import com.giathinh.uthconnect.domain.model.GenderPreference
import com.giathinh.uthconnect.domain.model.HousingListing
import com.giathinh.uthconnect.domain.model.HousingListingStatus
import com.giathinh.uthconnect.domain.model.HousingListingType
import com.giathinh.uthconnect.domain.model.UthCampus
import com.giathinh.uthconnect.domain.repository.HousingRepository
import com.giathinh.uthconnect.domain.usecase.GetMyHousingListingsUseCase
import com.giathinh.uthconnect.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MyListingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun listing(id: String, status: HousingListingStatus) = HousingListing(
        id = id,
        type = HousingListingType.ROOM_FOR_RENT,
        campus = UthCampus.CS1_QUANG_TRUNG,
        title = "Tin $id",
        description = "",
        hostName = "Chủ tin",
        monthlyRentVnd = 1_500_000,
        depositVnd = 0,
        utilitiesIncluded = true,
        availableSlots = 1,
        genderPreference = GenderPreference.ANY,
        scheduleNote = "",
        addressLabel = "Quanh trường",
        latitude = 10.83,
        longitude = 106.65,
        status = status,
        rejectionReason = if (status == HousingListingStatus.REJECTED) "Ảnh không rõ" else null,
    )

    @Test
    fun `mo man - tai du tin cua toi moi trang thai`() {
        val repo = FakeMyListingsRepository(
            listings = listOf(
                listing("HS-1", HousingListingStatus.PENDING),
                listing("HS-2", HousingListingStatus.OPEN),
                listing("HS-3", HousingListingStatus.REJECTED),
            ),
        )
        val viewModel = MyListingsViewModel(GetMyHousingListingsUseCase(repo))

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.failure)
        assertEquals(listOf("HS-1", "HS-2", "HS-3"), state.listings.map { it.id })
        assertEquals("Ảnh không rõ", state.listings[2].rejectionReason)
    }

    @Test
    fun `tai loi - hien trang thai loi khong phai danh sach rong`() {
        val repo = FakeMyListingsRepository(failLoad = true)
        val viewModel = MyListingsViewModel(GetMyHousingListingsUseCase(repo))

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(MyListingsFailure.LoadFailed, state.failure)
        assertFalse(state.isEmpty)
    }

    @Test
    fun `chua dang tin nao - trang thai rong`() {
        val repo = FakeMyListingsRepository(listings = emptyList())
        val viewModel = MyListingsViewModel(GetMyHousingListingsUseCase(repo))

        assertTrue(viewModel.uiState.value.isEmpty)
    }

    @Test
    fun `refresh - goi lai repository va cap nhat du lieu`() {
        val repo = FakeMyListingsRepository(listings = listOf(listing("HS-1", HousingListingStatus.PENDING)))
        val viewModel = MyListingsViewModel(GetMyHousingListingsUseCase(repo))
        repo.listings = listOf(listing("HS-1", HousingListingStatus.OPEN))

        viewModel.onAction(MyListingsAction.Refresh)

        assertEquals(2, repo.myListingsCalls)
        assertEquals(HousingListingStatus.OPEN, viewModel.uiState.value.listings.single().status)
    }

    @Test
    fun `OpenListing - phat event dieu huong detail`() = runTest {
        val repo = FakeMyListingsRepository(listings = listOf(listing("HS-1", HousingListingStatus.PENDING)))
        val viewModel = MyListingsViewModel(GetMyHousingListingsUseCase(repo))

        viewModel.onAction(MyListingsAction.OpenListing("HS-1"))

        assertEquals(MyListingsEvent.OpenListing("HS-1"), viewModel.events.first())
    }

    @Test
    fun `refresh loi khi da co du lieu - giu danh sach va bao event`() = runTest {
        val repo = FakeMyListingsRepository(listings = listOf(listing("HS-1", HousingListingStatus.OPEN)))
        val viewModel = MyListingsViewModel(GetMyHousingListingsUseCase(repo))
        repo.failLoad = true

        viewModel.onAction(MyListingsAction.Refresh)

        assertEquals(1, viewModel.uiState.value.listings.size)
        assertNull(viewModel.uiState.value.failure)
        assertEquals(MyListingsEvent.RefreshFailed, viewModel.events.first())
    }

    private class FakeMyListingsRepository(
        var listings: List<HousingListing> = emptyList(),
        var failLoad: Boolean = false,
    ) : HousingRepository {
        var myListingsCalls = 0

        override suspend fun getListings(): Result<List<HousingListing>> = Result.success(listings)

        override suspend fun getMyListings(): Result<List<HousingListing>> {
            myListingsCalls++
            return if (failLoad) Result.failure(IllegalStateException("mat mang")) else Result.success(listings)
        }

        override suspend fun getListingById(id: String): Result<HousingListing> {
            val error = NoSuchElementException(id)
            return Result.failure(error)
        }

        override suspend fun createListing(listing: HousingListing): Result<HousingListing> = Result.success(listing)

        override suspend fun updateListing(listing: HousingListing): Result<HousingListing> = Result.success(listing)

        override suspend fun markListingFulfilled(id: String): Result<Unit> = Result.success(Unit)

        override suspend fun deleteListing(id: String): Result<Unit> = Result.success(Unit)

        override suspend fun reportListing(listingId: String, reason: String): Result<Unit> = Result.success(Unit)
    }
}
