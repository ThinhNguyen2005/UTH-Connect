package com.giathinh.uthconnect.domain.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HousingListingTest {

    @Test
    fun `tin PENDING cho duyet - khong len feed`() {
        assertFalse(listing(status = HousingListingStatus.PENDING).isPublishable)
    }

    @Test
    fun `tin REJECTED bi tu choi - khong len feed`() {
        assertFalse(listing(status = HousingListingStatus.REJECTED).isPublishable)
    }

    @Test
    fun `tin FULFILLED het cho - khong len feed`() {
        assertFalse(listing(status = HousingListingStatus.FULFILLED).isPublishable)
    }

    @Test
    fun `tin HIDDEN_BY_REPORTS bi an - khong len feed`() {
        assertFalse(listing(status = HousingListingStatus.HIDDEN_BY_REPORTS).isPublishable)
    }

    @Test
    fun `tin OPEN con cho - len feed`() {
        assertTrue(listing(status = HousingListingStatus.OPEN).isPublishable)
    }

    @Test
    fun `tin OPEN het cho - khong len feed`() {
        assertFalse(listing(status = HousingListingStatus.OPEN, availableSlots = 0).isPublishable)
    }

    private fun listing(
        status: HousingListingStatus,
        availableSlots: Int = 1,
    ): HousingListing = HousingListing(
        type = HousingListingType.ROOM_FOR_RENT,
        campus = UthCampus.CS1_QUANG_TRUNG,
        title = "Tin kiểm thử",
        description = "",
        hostName = "Chủ tin",
        monthlyRentVnd = 1_500_000,
        depositVnd = 0,
        utilitiesIncluded = true,
        availableSlots = availableSlots,
        genderPreference = GenderPreference.ANY,
        scheduleNote = "",
        addressLabel = "Quanh trường",
        latitude = 10.83,
        longitude = 106.65,
        status = status,
    )
}
