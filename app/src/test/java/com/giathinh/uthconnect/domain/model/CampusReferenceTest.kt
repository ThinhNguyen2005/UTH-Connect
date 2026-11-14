package com.giathinh.uthconnect.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

    @Test
    fun `diem chuan hien tai - la reference point`() {
        UthCampus.entries.forEach { campus ->
            val (lat, lng) = CampusReference.of(campus)
            assertTrue(CampusReference.isReferencePoint(lat, lng))
        }
    }

    @Test
    fun `diem chuan cu truoc 11_11 - van nhan dien la reference point`() {
        assertTrue(CampusReference.isReferencePoint(10.8332, 106.6533))
        assertTrue(CampusReference.isReferencePoint(10.8698, 106.7176))
        assertTrue(CampusReference.isReferencePoint(10.8738, 106.6369))
    }

    @Test
    fun `toa do that gan truong - khong phai reference point`() {
        assertFalse(CampusReference.isReferencePoint(10.8025, 106.7150))
        assertFalse(CampusReference.isReferencePoint(0.0, 0.0))
    }
}
