package com.giathinh.uthconnect.presentation.utilities

import com.giathinh.uthconnect.presentation.utilities.campuses.CampusesUiState
import com.giathinh.uthconnect.presentation.utilities.settings.SettingsUiState
import com.giathinh.uthconnect.presentation.utilities.trainingpoint.TrainingPointUiState
import com.giathinh.uthconnect.presentation.utilities.tuition.TuitionUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Kiểm thử hợp đồng dữ liệu cho các màn hình tiện ích con:
 * Học phí, Điểm rèn luyện, Cơ sở UTH và Cài đặt.
 */
class UtilitiesSubScreensTest {

    @Test
    fun `TuitionUiState mac dinh co du thong tin hoc phi va hoc phan`() {
        val state = TuitionUiState()
        assertTrue("Học phí mặc định là đã thanh toán", state.isPaid)
        assertEquals("0 VNĐ", state.remainingAmount)
        assertEquals(18, state.creditsCount)
        assertFalse("Danh sách môn học không được rỗng", state.courses.isEmpty())
    }

    @Test
    fun `TrainingPointUiState mac dinh co diem hop le va du 5 tieu chi`() {
        val state = TrainingPointUiState()
        assertEquals(88, state.totalScore)
        assertEquals(100, state.maxScore)
        assertEquals("TỐT", state.rank)
        assertEquals("Phải có 5 tiêu chí rèn luyện", 5, state.criteriaList.size)
    }

    @Test
    fun `CampusesUiState co du 3 co so va danh ba phong ban`() {
        val state = CampusesUiState()
        assertEquals("Phải có đủ 3 cơ sở đào tạo", 3, state.campuses.size)
        assertTrue(state.campuses.any { it.name.contains("Cơ sở 1") })
        assertTrue(state.campuses.any { it.name.contains("Cơ sở 2") })
        assertTrue(state.campuses.any { it.name.contains("Cơ sở 3") })
        assertFalse("Danh bạ phòng ban không được rỗng", state.departments.isEmpty())
    }

    @Test
    fun `SettingsUiState mac dinh co phien ban 1_0_0`() {
        val state = SettingsUiState()
        assertEquals("1.0.0", state.appVersion)
        assertTrue("Mặc định bật nhắc nhở học phí", state.tuitionReminderEnabled)
        assertTrue("Mặc định bật thông báo rèn luyện", state.trainingPointNotifEnabled)
    }
}
