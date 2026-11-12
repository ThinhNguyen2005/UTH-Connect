package com.giathinh.uthconnect.presentation.housing.create

import com.giathinh.uthconnect.domain.model.GenderPreference
import com.giathinh.uthconnect.domain.model.HousingListingType
import com.giathinh.uthconnect.domain.model.RoomAmenity
import com.giathinh.uthconnect.domain.model.UthCampus

enum class CreateHousingPostError {
    EMPTY_TITLE,
    INVALID_TITLE_LENGTH,
    INVALID_RENT,
    INVALID_AREA,
    EMPTY_ADDRESS,
    EMPTY_PHONE,
    SIGN_IN_REQUIRED,
}

/** Trần số ảnh phòng cho một tin — khớp ô "Tối đa 6" của form và chặn AddPhoto vượt trần. */
internal const val MAX_HOUSING_PHOTOS = 6

data class CreateHousingPostUiState(
    val type: HousingListingType = HousingListingType.ROOM_FOR_RENT,
    val campus: UthCampus = UthCampus.CS1_QUANG_TRUNG,
    val title: String = "",
    val description: String = "",
    val rentInput: String = "",
    val depositInput: String = "",
    val utilitiesIncluded: Boolean = false,
    val address: String = "",
    /** Tọa độ thật lấy từ picker bản đồ; (0,0) = chưa chấm pin, repository tự điền điểm cơ sở. */
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val areaInput: String = "",
    val slots: Int = 1,
    val genderPreference: GenderPreference = GenderPreference.ANY,
    val scheduleNote: String = "",
    val selectedAmenities: Set<RoomAmenity> = emptySet(),
    val contactPhone: String = "",
    val contactZalo: String = "",
    /** Cho phép người xem liên hệ qua Zalo; tắt thì ẩn ô Zalo và không gửi kèm link khi đăng. */
    val allowZalo: Boolean = true,
    val photos: List<String> = emptyList(),
    val isSubmitting: Boolean = false,
    val validationError: CreateHousingPostError? = null,
) {
    /** Đã chấm pin thật trên bản đồ — hiện tóm tắt "đã chọn vị trí" thay vì chỉ ô địa chỉ tay. */
    val hasLocation: Boolean get() = latitude != 0.0 || longitude != 0.0
}
