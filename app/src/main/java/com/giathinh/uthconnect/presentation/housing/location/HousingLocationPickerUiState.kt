package com.giathinh.uthconnect.presentation.housing.location

import com.giathinh.uthconnect.domain.model.CampusReference
import com.giathinh.uthconnect.domain.model.UthCampus

/**
 * Lỗi chặn luồng chọn vị trí, dạng enum — Screen quyết định in câu nào
 * (docs/CODING_STANDARDS.md mục 3.3). Từ chối quyền GPS hoặc GPS hỏng đều quy về
 * [LocationUnavailable] vì lối thoát giống nhau: chấm pin tay trên bản đồ.
 */
enum class HousingLocationPickerFailure {
    LocationUnavailable,
    GeocodeFailed,
}

/**
 * Tâm bản đồ mặc định là CS1 để mở màn đã thấy ngay khu quanh trường;
 * [hasPickedLocation] chỉ true sau khi chủ tin thật sự kéo pin hoặc bấm GPS —
 * nút Xong khóa cho tới lúc đó để không đăng nhầm "vị trí phòng = giữa sân trường".
 * [gpsFixCount] tăng mỗi lần GPS trả kết quả để Screen đẩy camera theo một lần,
 * tách khỏi cập nhật tọa độ do kéo bản đồ (tránh giật camera khi đang kéo).
 */
data class HousingLocationPickerUiState(
    val latitude: Double = CampusReference.of(UthCampus.CS1_QUANG_TRUNG).first,
    val longitude: Double = CampusReference.of(UthCampus.CS1_QUANG_TRUNG).second,
    val address: String = "",
    val isLocating: Boolean = false,
    val isGeocoding: Boolean = false,
    val hasPickedLocation: Boolean = false,
    val gpsFixCount: Int = 0,
    val failure: HousingLocationPickerFailure? = null,
)
