package com.giathinh.uthconnect.presentation.housing.location

import com.giathinh.uthconnect.domain.model.CampusReference
import com.giathinh.uthconnect.domain.model.UthCampus
import com.giathinh.uthconnect.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HousingLocationPickerViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `mac dinh - chua chon vi tri va tam ban do o CS1`() {
        val state = HousingLocationPickerViewModel().uiState.value

        assertFalse(state.hasPickedLocation)
        assertEquals(0, state.gpsFixCount)
        assertNull(state.failure)
        val (lat, lng) = CampusReference.of(UthCampus.CS1_QUANG_TRUNG)
        assertEquals(lat, state.latitude, 0.0)
        assertEquals(lng, state.longitude, 0.0)
    }

    @Test
    fun `PinMoved - cap nhat toa do va danh dau da chon`() {
        val viewModel = HousingLocationPickerViewModel()

        viewModel.onAction(HousingLocationPickerAction.PinMoved(10.8051, 106.7170))

        val state = viewModel.uiState.value
        assertEquals(10.8051, state.latitude, 0.0)
        assertEquals(106.7170, state.longitude, 0.0)
        assertTrue(state.hasPickedLocation)
        assertTrue(state.isGeocoding)
    }

    @Test
    fun `UseCurrentLocation - bat trang thai dang lay GPS`() {
        val viewModel = HousingLocationPickerViewModel()

        viewModel.onAction(HousingLocationPickerAction.UseCurrentLocation)

        assertTrue(viewModel.uiState.value.isLocating)
    }

    @Test
    fun `CurrentLocationResult - tat GPS tang gpsFixCount va danh dau da chon`() {
        val viewModel = HousingLocationPickerViewModel()

        viewModel.onAction(HousingLocationPickerAction.CurrentLocationResult(10.8051, 106.7170))

        val state = viewModel.uiState.value
        assertFalse(state.isLocating)
        assertTrue(state.hasPickedLocation)
        assertEquals(1, state.gpsFixCount)
        assertEquals(10.8051, state.latitude, 0.0)
    }

    @Test
    fun `CurrentLocationResult hai lan - gpsFixCount tang hai de camera theo mot lan`() {
        val viewModel = HousingLocationPickerViewModel()

        viewModel.onAction(HousingLocationPickerAction.CurrentLocationResult(10.8051, 106.7170))
        viewModel.onAction(HousingLocationPickerAction.CurrentLocationResult(10.8060, 106.7180))

        assertEquals(2, viewModel.uiState.value.gpsFixCount)
    }

    @Test
    fun `CurrentLocationFailed - bao LocationUnavailable va tat GPS`() {
        val viewModel = HousingLocationPickerViewModel()
        viewModel.onAction(HousingLocationPickerAction.UseCurrentLocation)

        viewModel.onAction(HousingLocationPickerAction.CurrentLocationFailed)

        val state = viewModel.uiState.value
        assertFalse(state.isLocating)
        assertEquals(HousingLocationPickerFailure.LocationUnavailable, state.failure)
        assertFalse(state.hasPickedLocation)
    }

    @Test
    fun `AddressGeocoded - dien dia chi va tat trang thai dich`() {
        val viewModel = HousingLocationPickerViewModel()
        viewModel.onAction(HousingLocationPickerAction.PinMoved(10.8051, 106.7170))

        viewModel.onAction(HousingLocationPickerAction.AddressGeocoded("138 Võ Oanh, Bình Thạnh"))

        val state = viewModel.uiState.value
        assertEquals("138 Võ Oanh, Bình Thạnh", state.address)
        assertFalse(state.isGeocoding)
    }

    @Test
    fun `GeocodeFailed - bao GeocodeFailed nhung van giu vi tri da chon`() {
        val viewModel = HousingLocationPickerViewModel()
        viewModel.onAction(HousingLocationPickerAction.PinMoved(10.8051, 106.7170))

        viewModel.onAction(HousingLocationPickerAction.GeocodeFailed)

        val state = viewModel.uiState.value
        assertEquals(HousingLocationPickerFailure.GeocodeFailed, state.failure)
        assertFalse(state.isGeocoding)
        assertTrue(state.hasPickedLocation)
    }

    @Test
    fun `ChangeAddress - cho phep sua tay dia chi`() {
        val viewModel = HousingLocationPickerViewModel()

        viewModel.onAction(HousingLocationPickerAction.ChangeAddress("Số 1 Võ Oanh"))

        assertEquals("Số 1 Võ Oanh", viewModel.uiState.value.address)
    }

    @Test
    fun `Confirm khi chua chon vi tri - khong phat event`() = runTest {
        val viewModel = HousingLocationPickerViewModel()
        val received = mutableListOf<HousingLocationPickerEvent>()
        val job = launch { viewModel.events.toList(received) }

        viewModel.onAction(HousingLocationPickerAction.Confirm)
        advanceUntilIdle()

        assertTrue("chua chon vi tri thi khong duoc chot", received.isEmpty())
        job.cancel()
    }

    @Test
    fun `Confirm sau khi chon - phat LocationConfirmed voi dia chi da trim`() = runTest {
        val viewModel = HousingLocationPickerViewModel()
        viewModel.onAction(HousingLocationPickerAction.PinMoved(10.8051, 106.7170))
        viewModel.onAction(HousingLocationPickerAction.AddressGeocoded("  138 Võ Oanh  "))

        viewModel.onAction(HousingLocationPickerAction.Confirm)

        val event = viewModel.events.first()
        assertEquals(HousingLocationPickerEvent.LocationConfirmed(10.8051, 106.7170, "138 Võ Oanh"), event)
    }

    @Test
    fun `Confirm khi dang geocode - khong phat event`() = runTest {
        val viewModel = HousingLocationPickerViewModel()
        val received = mutableListOf<HousingLocationPickerEvent>()
        val job = launch { viewModel.events.toList(received) }

        viewModel.onAction(HousingLocationPickerAction.PinMoved(10.8051, 106.7170))
        viewModel.onAction(HousingLocationPickerAction.Confirm)
        advanceUntilIdle()

        assertTrue("dang dich dia chi thi khong duoc chot", received.isEmpty())
        job.cancel()
    }

    @Test
    fun `PinMoved - xoa dia chi cu de khong lech toa do`() {
        val viewModel = HousingLocationPickerViewModel()
        viewModel.onAction(HousingLocationPickerAction.PinMoved(10.8051, 106.7170))
        viewModel.onAction(HousingLocationPickerAction.AddressGeocoded("138 Võ Oanh, Bình Thạnh"))

        viewModel.onAction(HousingLocationPickerAction.PinMoved(10.8060, 106.7180))

        val state = viewModel.uiState.value
        assertEquals("", state.address)
        assertTrue(state.isGeocoding)
    }
}
