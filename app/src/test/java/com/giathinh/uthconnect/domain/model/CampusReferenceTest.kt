package com.giathinh.uthconnect.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CampusReferenceTest {

    @Test
    fun `trung toa do co so - khoang cach bang 0`() {
        val (lat, lng) = CampusReference.of(UthCampus.CS1_QUANG_TRUNG)

        assertEquals(0, CampusReference.distanceMeters(UthCampus.CS1_QUANG_TRUNG, lat, lng))
    }

    @Test
    fun `lech 0_01 do vi - xap xi 1112m theo cong thuc haversine`() {
        val (lat, lng) = CampusReference.of(UthCampus.CS1_QUANG_TRUNG)

        val meters = CampusReference.distanceMeters(UthCampus.CS1_QUANG_TRUNG, lat + 0.01, lng)

        assertTrue("0.01° vĩ tuyến ≈ 1112m, thực tế $meters", meters in 1_100..1_125)
    }

    @Test
    fun `toa do placeholder 0_0 - tra 0 thay vi so rac`() {
        assertEquals(0, CampusReference.distanceMeters(UthCampus.CS1_QUANG_TRUNG, 0.0, 0.0))
    }

    @Test
    fun `toa do ngoai pham vi - tra 0`() {
        assertEquals(0, CampusReference.distanceMeters(UthCampus.CS2_THU_DUC, 91.0, 106.7))
        assertEquals(0, CampusReference.distanceMeters(UthCampus.CS2_THU_DUC, 10.87, 181.0))
    }
}
