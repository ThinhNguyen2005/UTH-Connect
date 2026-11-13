package com.giathinh.uthconnect.presentation.housing.create

import com.giathinh.uthconnect.domain.model.GenderPreference
import com.giathinh.uthconnect.domain.model.HousingListingType
import com.giathinh.uthconnect.domain.model.RoomAmenity
import com.giathinh.uthconnect.domain.model.UthCampus
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

enum class CreateHousingPostError {
    EMPTY_TITLE,
    INVALID_TITLE_LENGTH,
    INVALID_RENT,
    INVALID_DEPOSIT,
    INVALID_AREA,
    EMPTY_ADDRESS,
    EMPTY_PHONE,
    INVALID_ZALO,
    SIGN_IN_REQUIRED,
}

/** Trần số ảnh phòng cho một tin — khớp ô "Tối đa 6" của form và chặn AddPhoto vượt trần. */
internal const val MAX_HOUSING_PHOTOS = 6

/** Serialize để lưu nháp cục bộ; hai trường trạng thái tức thời không đi theo nháp. */
@Serializable
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
    /**
     * ID tin cho lượt gửi hiện tại — giữ nguyên khi gửi lại sau lỗi (response
     * mất nhưng server có thể đã commit) để không nhân đôi tin; mọi sửa đổi
     * form xóa ID này. Đi theo nháp để khôi phục xong gửi lại vẫn đúng ID.
     */
    val submitId: String = "",
    @Transient val isSubmitting: Boolean = false,
    @Transient val validationError: CreateHousingPostError? = null,
    /** Tăng mỗi lần gửi lỗi để màn hình cuộn lại banner dù mã lỗi không đổi. */
    @Transient val validationNonce: Int = 0,
) {
    /** Đã chấm pin thật trên bản đồ — hiện tóm tắt "đã chọn vị trí" thay vì chỉ ô địa chỉ tay. */
    val hasLocation: Boolean get() = latitude != 0.0 || longitude != 0.0
}
