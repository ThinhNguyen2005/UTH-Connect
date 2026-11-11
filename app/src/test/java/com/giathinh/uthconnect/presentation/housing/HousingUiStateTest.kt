package com.giathinh.uthconnect.presentation.housing

import com.giathinh.uthconnect.domain.model.GenderPreference
import com.giathinh.uthconnect.domain.model.HousingListing
import com.giathinh.uthconnect.domain.model.HousingListingStatus
import com.giathinh.uthconnect.domain.model.HousingListingType
import com.giathinh.uthconnect.domain.model.HousingPriceRange
import com.giathinh.uthconnect.domain.model.UthCampus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Bộ lọc nằm trong UiState nên test JVM thuần, không cần dispatcher hay thiết bị. */
class HousingUiStateTest {

    @Test
    fun `tin fulfilled het cho hoac bi an - khong len feed`() {
        val state = HousingUiState(
            isLoading = false,
            listings = listOf(
                listing("open"),
                listing("fulfilled", status = HousingListingStatus.FULFILLED),
                listing("full", availableSlots = 0),
                listing("hidden", status = HousingListingStatus.HIDDEN_BY_REPORTS),
            ),
        )

        assertEquals(listOf("open"), state.filteredListings.map { it.id })
    }

    @Test
    fun `loc theo type va campus`() {
        val state = HousingUiState(
            isLoading = false,
            listings = listOf(
                listing("rent-cs1", type = HousingListingType.ROOM_FOR_RENT),
                listing("mate-cs1", type = HousingListingType.LOOKING_FOR_ROOMMATE),
                listing("rent-cs2", type = HousingListingType.ROOM_FOR_RENT, campus = UthCampus.CS2_THU_DUC),
            ),
            selectedType = HousingListingType.ROOM_FOR_RENT,
            selectedCampus = UthCampus.CS1_QUANG_TRUNG,
        )

        assertEquals(listOf("rent-cs1"), state.filteredListings.map { it.id })
    }

    @Test
    fun `loc theo gia va khoang cach`() {
        val state = HousingUiState(
            isLoading = false,
            listings = listOf(
                listing("cheap-near", monthlyRentVnd = 1_500_000, distanceToCampusM = 400),
                listing("cheap-far", monthlyRentVnd = 1_500_000, distanceToCampusM = 2_500),
                listing("expensive-near", monthlyRentVnd = 3_000_000, distanceToCampusM = 400),
            ),
            maxRentVnd = 2_000_000,
            maxDistanceM = 1_000,
        )

        assertEquals(listOf("cheap-near"), state.filteredListings.map { it.id })
    }

    @Test
    fun `loc nhanh theo 3 khoang gia cua Jira`() {
        val listings = listOf(
            listing("under2m", monthlyRentVnd = 1_800_000),
            listing("mid2to35", monthlyRentVnd = 2_800_000),
            listing("over35", monthlyRentVnd = 4_000_000),
        )

        val underState = HousingUiState(isLoading = false, listings = listings, priceRange = HousingPriceRange.UNDER_2M)
        assertEquals(listOf("under2m"), underState.filteredListings.map { it.id })

        val midState = HousingUiState(
            isLoading = false,
            listings = listings,
            priceRange = HousingPriceRange.FROM_2M_TO_3_5M,
        )
        assertEquals(listOf("mid2to35"), midState.filteredListings.map { it.id })

        val overState = HousingUiState(isLoading = false, listings = listings, priceRange = HousingPriceRange.OVER_3_5M)
        assertEquals(listOf("over35"), overState.filteredListings.map { it.id })
    }

    @Test
    fun `gioi tinh loc hai chieu - tin chi nhan nam khong hien khi loc nu`() {
        val state = HousingUiState(
            isLoading = false,
            listings = listOf(
                listing("male-only", genderPreference = GenderPreference.MALE),
                listing("female-only", genderPreference = GenderPreference.FEMALE),
                listing("any", genderPreference = GenderPreference.ANY),
            ),
            genderPreference = GenderPreference.FEMALE,
        )

        assertEquals(listOf("female-only", "any"), state.filteredListings.map { it.id })
    }

    @Test
    fun `sort NEAREST_CAMPUS va CHEAPEST`() {
        val listings = listOf(
            listing("far-expensive", monthlyRentVnd = 3_000_000, distanceToCampusM = 2_000),
            listing("near-cheap", monthlyRentVnd = 1_000_000, distanceToCampusM = 300),
            listing("mid", monthlyRentVnd = 2_000_000, distanceToCampusM = 1_000),
        )

        val nearest = HousingUiState(isLoading = false, listings = listings, sort = HousingSort.NEAREST_CAMPUS)
        assertEquals(listOf("near-cheap", "mid", "far-expensive"), nearest.filteredListings.map { it.id })

        val cheapest = HousingUiState(isLoading = false, listings = listings, sort = HousingSort.CHEAPEST)
        assertEquals(listOf("near-cheap", "mid", "far-expensive"), cheapest.filteredListings.map { it.id })
    }

    @Test
    fun `rong do loc khac rong vi chua ai dang`() {
        val filtered = HousingUiState(
            isLoading = false,
            listings = listOf(listing("rent", type = HousingListingType.ROOM_FOR_RENT)),
            selectedType = HousingListingType.LOOKING_FOR_ROOM,
        )
        assertTrue(filtered.isEmpty)
        assertTrue(filtered.isEmptyBecauseOfFilters)

        val trulyEmpty = HousingUiState(isLoading = false)
        assertTrue(trulyEmpty.isEmpty)
        assertFalse(trulyEmpty.isEmptyBecauseOfFilters)
    }

    @Test
    fun `hasActiveFilters chi tinh khi co it nhat mot bo loc`() {
        assertFalse(HousingUiState().hasActiveFilters)
        assertTrue(HousingUiState(maxDistanceM = 1_000).hasActiveFilters)
        assertFalse("sort khong phai bo loc", HousingUiState(sort = HousingSort.CHEAPEST).hasActiveFilters)
    }

    @Test
    fun `searchQuery va savedListingIds mac dinh rong - chua loc chua luu`() {
        val initial = HousingUiState()

        assertEquals("", initial.searchQuery)
        assertTrue(initial.savedListingIds.isEmpty())
        assertFalse(initial.hasActiveFilters)
    }

    @Test
    fun `tim kiem theo tieu de - khong phan biet hoa thuong`() {
        val state = HousingUiState(
            isLoading = false,
            listings = listOf(
                listing("gac-lung", title = "Phòng gác lửng gần CS1"),
                listing("ban-cong", title = "Phòng ban công thoáng mát"),
            ),
            searchQuery = "GÁC LỬNG",
        )

        assertEquals(listOf("gac-lung"), state.filteredListings.map { it.id })
    }

    @Test
    fun `tim kiem theo dia chi khi tieu de khong khop`() {
        val state = HousingUiState(
            isLoading = false,
            listings = listOf(
                listing("quang-trung", title = "Phòng A", addressLabel = "138 Quang Trung, Gò Vấp"),
                listing("le-van-viet", title = "Phòng B", addressLabel = "Lê Văn Việt, Thủ Đức"),
            ),
            searchQuery = "quang trung",
        )

        assertEquals(listOf("quang-trung"), state.filteredListings.map { it.id })
    }

    @Test
    fun `tim kiem toan khoang trang - coi nhu khong tim`() {
        val state = HousingUiState(
            isLoading = false,
            listings = listOf(listing("a"), listing("b")),
            searchQuery = "   ",
        )

        assertEquals(2, state.filteredListings.size)
        assertFalse("khoang trang khong phai bo loc", state.hasActiveFilters)
    }

    @Test
    fun `tim kiem khong khop - rong vi loc va dem visible bang 0`() {
        val state = HousingUiState(
            isLoading = false,
            listings = listOf(listing("a")),
            searchQuery = "không tồn tại",
        )

        assertTrue(state.isEmpty)
        assertTrue(state.isEmptyBecauseOfFilters)
        assertEquals(0, state.visibleCount)
    }

    @Test
    fun `tim kiem ket hop bo loc campus - ca hai dieu cung phai khop`() {
        val state = HousingUiState(
            isLoading = false,
            listings = listOf(
                listing("cs1-khop", title = "Phòng gác lửng", campus = UthCampus.CS1_QUANG_TRUNG),
                listing("cs2-khop-tu", title = "Phòng gác lửng", campus = UthCampus.CS2_THU_DUC),
                listing("cs1-khac-tu", title = "Phòng ban công", campus = UthCampus.CS1_QUANG_TRUNG),
            ),
            selectedCampus = UthCampus.CS1_QUANG_TRUNG,
            searchQuery = "gác lửng",
        )

        assertEquals(listOf("cs1-khop"), state.filteredListings.map { it.id })
    }

    private fun listing(
        id: String,
        type: HousingListingType = HousingListingType.ROOM_FOR_RENT,
        campus: UthCampus = UthCampus.CS1_QUANG_TRUNG,
        monthlyRentVnd: Int = 2_000_000,
        distanceToCampusM: Int = 500,
        genderPreference: GenderPreference = GenderPreference.ANY,
        availableSlots: Int = 1,
        status: HousingListingStatus = HousingListingStatus.OPEN,
        title: String = "Tin $id",
        addressLabel: String = "Quanh trường",
    ): HousingListing = HousingListing(
        id = id,
        type = type,
        campus = campus,
        title = title,
        description = "",
        hostName = "Chủ tin",
        monthlyRentVnd = monthlyRentVnd,
        depositVnd = 0,
        utilitiesIncluded = true,
        availableSlots = availableSlots,
        genderPreference = genderPreference,
        scheduleNote = "",
        addressLabel = addressLabel,
        latitude = 10.83,
        longitude = 106.65,
        distanceToCampusM = distanceToCampusM,
        status = status,
    )
}
