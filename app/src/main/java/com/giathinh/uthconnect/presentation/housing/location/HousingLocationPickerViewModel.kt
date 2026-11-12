package com.giathinh.uthconnect.presentation.housing.location

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface HousingLocationPickerEvent {
    data class LocationConfirmed(
        val latitude: Double,
        val longitude: Double,
        val address: String,
    ) : HousingLocationPickerEvent
}

@HiltViewModel
class HousingLocationPickerViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(HousingLocationPickerUiState())
    val uiState: StateFlow<HousingLocationPickerUiState> = _uiState.asStateFlow()

    private val _events = Channel<HousingLocationPickerEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    /** GPS chỉ áp dụng khi yêu cầu còn hiệu lực — kéo pin giữa chừng hủy kết quả GPS đến muộn. */
    private var gpsRequestPending = false

    /** Người dùng sửa địa chỉ tay trong lúc geocode chạy thì kết quả geocode không được đè lên. */
    private var addressEditedManually = false

    fun onAction(action: HousingLocationPickerAction) {
        when (action) {
            is HousingLocationPickerAction.PinMoved -> {
                gpsRequestPending = false
                addressEditedManually = false
                _uiState.update {
                    it.copy(
                        latitude = action.latitude,
                        longitude = action.longitude,
                        hasPickedLocation = true,
                        isGeocoding = true,
                        isLocating = false,
                        address = "",
                        failure = null,
                    )
                }
            }

            HousingLocationPickerAction.UseCurrentLocation -> {
                gpsRequestPending = true
                _uiState.update { it.copy(isLocating = true, failure = null) }
            }

            is HousingLocationPickerAction.CurrentLocationResult -> applyCurrentLocation(action)

            HousingLocationPickerAction.CurrentLocationFailed -> applyLocationFailure()

            is HousingLocationPickerAction.AddressGeocoded -> {
                val keepManualAddress = addressEditedManually
                _uiState.update {
                    if (keepManualAddress) {
                        it.copy(isGeocoding = false)
                    } else {
                        it.copy(address = action.address, isGeocoding = false)
                    }
                }
            }

            HousingLocationPickerAction.GeocodeFailed ->
                _uiState.update { it.copy(isGeocoding = false, failure = HousingLocationPickerFailure.GeocodeFailed) }

            is HousingLocationPickerAction.ChangeAddress -> {
                addressEditedManually = true
                _uiState.update { it.copy(address = action.address) }
            }

            HousingLocationPickerAction.Confirm -> confirm()
        }
    }

    private fun applyCurrentLocation(action: HousingLocationPickerAction.CurrentLocationResult) {
        if (!gpsRequestPending) return
        gpsRequestPending = false
        addressEditedManually = false
        _uiState.update {
            it.copy(
                latitude = action.latitude,
                longitude = action.longitude,
                isLocating = false,
                hasPickedLocation = true,
                isGeocoding = true,
                address = "",
                gpsFixCount = it.gpsFixCount + 1,
                failure = null,
            )
        }
    }

    private fun applyLocationFailure() {
        if (!gpsRequestPending) return
        gpsRequestPending = false
        _uiState.update { it.copy(isLocating = false, failure = HousingLocationPickerFailure.LocationUnavailable) }
    }

    /** Chỉ chốt khi có vị trí thật, geocode xong và địa chỉ không rỗng — cho phép nhập tay thay geocode. */
    private fun confirm() {
        val state = _uiState.value
        if (!state.hasPickedLocation || state.isGeocoding || state.address.isBlank()) return
        viewModelScope.launch {
            _events.send(
                HousingLocationPickerEvent.LocationConfirmed(state.latitude, state.longitude, state.address.trim()),
            )
        }
    }
}
