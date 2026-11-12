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

    fun onAction(action: HousingLocationPickerAction) {
        when (action) {
            is HousingLocationPickerAction.PinMoved -> _uiState.update {
                it.copy(
                    latitude = action.latitude,
                    longitude = action.longitude,
                    hasPickedLocation = true,
                    isGeocoding = true,
                    failure = null,
                )
            }

            HousingLocationPickerAction.UseCurrentLocation ->
                _uiState.update { it.copy(isLocating = true, failure = null) }

            is HousingLocationPickerAction.CurrentLocationResult -> _uiState.update {
                it.copy(
                    latitude = action.latitude,
                    longitude = action.longitude,
                    isLocating = false,
                    hasPickedLocation = true,
                    isGeocoding = true,
                    gpsFixCount = it.gpsFixCount + 1,
                    failure = null,
                )
            }

            HousingLocationPickerAction.CurrentLocationFailed -> _uiState.update {
                it.copy(isLocating = false, failure = HousingLocationPickerFailure.LocationUnavailable)
            }

            is HousingLocationPickerAction.AddressGeocoded ->
                _uiState.update { it.copy(address = action.address, isGeocoding = false) }

            HousingLocationPickerAction.GeocodeFailed ->
                _uiState.update { it.copy(isGeocoding = false, failure = HousingLocationPickerFailure.GeocodeFailed) }

            is HousingLocationPickerAction.ChangeAddress ->
                _uiState.update { it.copy(address = action.address) }

            HousingLocationPickerAction.Confirm -> confirm()
        }
    }

    /** Chỉ chốt khi đã có vị trí thật — chặn đăng nhầm điểm mặc định giữa sân trường. */
    private fun confirm() {
        val state = _uiState.value
        if (!state.hasPickedLocation) return
        viewModelScope.launch {
            _events.send(
                HousingLocationPickerEvent.LocationConfirmed(state.latitude, state.longitude, state.address.trim()),
            )
        }
    }
}
