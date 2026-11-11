package com.giathinh.uthconnect.presentation.housing.create

import com.giathinh.uthconnect.domain.model.GenderPreference
import com.giathinh.uthconnect.domain.model.HousingListingType
import com.giathinh.uthconnect.domain.model.RoomAmenity
import com.giathinh.uthconnect.domain.model.UthCampus

enum class CreateHousingPostError {
    EMPTY_TITLE,
    INVALID_RENT,
    EMPTY_AREA,
    EMPTY_EXACT_ADDRESS,
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
    /** Khu vực công khai (phường/quận) — hiển thị feed trước khi reveal. */
    val areaLabel: String = "",
    /** Địa chỉ đầy đủ — chỉ vào bảng private, mở sau contact request ACCEPTED. */
    val exactAddress: String = "",
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
)
