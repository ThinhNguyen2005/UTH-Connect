package com.giathinh.uthconnect.presentation.housing.location

sealed interface HousingLocationPickerAction {
    /** Bản đồ dừng kéo — chốt pin tại tâm màn hình. */
    data class PinMoved(val latitude: Double, val longitude: Double) : HousingLocationPickerAction

    /** Route xin quyền + gọi GPS rồi trả kết quả bằng hai action dưới. */
    data object UseCurrentLocation : HousingLocationPickerAction
    data class CurrentLocationResult(val latitude: Double, val longitude: Double) : HousingLocationPickerAction
    data object CurrentLocationFailed : HousingLocationPickerAction

    /** Geocoder dịch xong tọa độ → địa chỉ (Route chạy trên IO). */
    data class AddressGeocoded(val address: String) : HousingLocationPickerAction
    data object GeocodeFailed : HousingLocationPickerAction

    data class ChangeAddress(val address: String) : HousingLocationPickerAction
    data object Confirm : HousingLocationPickerAction
}
