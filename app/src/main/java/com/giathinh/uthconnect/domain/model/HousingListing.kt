package com.giathinh.uthconnect.domain.model

/*
 * Mô hình Ghép trọ theo `README.md` mục 3.2: tin phòng/ở ghép kèm giá, khoảng cách
 * tới cơ sở và nhu cầu bạn ở ghép.
 *
 * Dòng public chỉ mang tọa độ xấp xỉ (`approxLatitude`/`approxLongitude`, server
 * làm tròn ≈ lưới 1km) nuôi bộ lọc "khoảng cách tới cơ sở"; `addressLabel` là khu
 * vực công khai. Tọa độ chính xác, địa chỉ đầy đủ và SĐT/Zalo nằm ở
 * [HousingPrivateDetails], chỉ mở sau contact request ACCEPTED.
 */

/** Ba ý định khác nhau trong cùng module — gộp lại thành một là nhầm luồng (README 3.2). */
enum class HousingListingType {
    /** Chủ phòng rao phòng cho thuê. */
    ROOM_FOR_RENT,

    /** Sinh viên đã có phòng, cần tìm người ở ghép. */
    LOOKING_FOR_ROOMMATE,

    /** Sinh viên cần tìm phòng/ở ghép, chưa có chỗ ở. */
    LOOKING_FOR_ROOM,
}

/** Cơ sở UTH dùng làm gốc tính khoảng cách — trùng nhãn với `events.campus`. */
enum class UthCampus {
    CS1_QUANG_TRUNG,
    CS2_THU_DUC,
    CS3_TO_KY,
}

/**
 * Vòng đời tin. `hiddenByReports` là cơ chế chống lạm dụng "ẩn sau 3 báo cáo"
 * (README 3.2 mục Chống lạm dụng) do backend quyết, không phải lựa chọn của chủ tin.
 */
enum class HousingListingStatus {
    OPEN,
    FULFILLED,
    HIDDEN_BY_REPORTS,
}

/** Giới tính người ở ghép mong muốn; `ANY` = không đặt điều kiện. */
enum class GenderPreference {
    ANY,
    MALE,
    FEMALE,
}

/** Cách danh tính chủ tin được xác thực, hiển thị thành badge trên mọi card (README 3.2). */
enum class HostVerificationMethod {
    EMAIL_UTH,
    MSSV,
}

/** Tiện nghi khai dạng có/không — đủ cho bộ lọc nhanh, không cần danh sách mở. */
enum class RoomAmenity {
    MEZZANINE,
    AIR_CONDITIONER,
    MACHINE_WASH,
    FREE_HOURS,
    GOVERNMENT_UTILITY_RATES,
    SECURED_PARKING,
    COOKING,
    HIGH_SPEED_WIFI,
    HOT_WATER,
    BALCONY,
    KEY_CARD,
}

/**
 * Ba khoảng giá lọc nhanh chuẩn Jira:
 * < 2 triệu, 2 - 3.5 triệu, > 3.5 triệu.
 */
enum class HousingPriceRange {
    ALL,
    UNDER_2M,
    FROM_2M_TO_3_5M,
    OVER_3_5M,
    ;

    fun matches(rentVnd: Int): Boolean = when (this) {
        ALL -> true
        UNDER_2M -> rentVnd < RENT_BAND_2M
        FROM_2M_TO_3_5M -> rentVnd in RENT_BAND_2M..RENT_BAND_3_5M
        OVER_3_5M -> rentVnd > RENT_BAND_3_5M
    }

    companion object {
        /** Ngưỡng giá nhanh (VND) — khớp nhãn "2 triệu" / "3.5 triệu" trên chip lọc feed. */
        private const val RENT_BAND_2M = 2_000_000
        private const val RENT_BAND_3_5M = 3_500_000
    }
}

data class HousingListing(
    val id: String = "",
    val type: HousingListingType,
    val campus: UthCampus,
    val title: String,
    val description: String,
    val hostName: String,
    /** MSSV chủ tin để hiển thị trên detail card chống lừa đảo (Jira). */
    val hostStudentId: String = "",
    /** Danh sách đường dẫn ảnh phòng (Carousel). */
    val images: List<String> = emptyList(),
    /** Giá thuê một tháng, đơn vị VND (Design-system mục 2: `1.500.000đ`). */
    val monthlyRentVnd: Int,
    val depositVnd: Int,
    /** `true` = tiền điện nước đã nằm trong giá thuê. */
    val utilitiesIncluded: Boolean,
    /** Số chỗ còn trống; 0 là tin hết hạn sử dụng và nên ở trạng thái `FULFILLED`. */
    val availableSlots: Int,
    val genderPreference: GenderPreference,
    /** Giờ giấc sinh hoạt, hút thuốc, thú cưng… — spec để dạng text tự do. */
    val scheduleNote: String,
    val amenities: List<RoomAmenity> = emptyList(),
    val addressLabel: String,
    /** Tọa độ xấp xỉ server làm tròn; 0/0 = tin chưa gắn vị trí. */
    val approxLatitude: Double = 0.0,
    val approxLongitude: Double = 0.0,
    /**
     * Mét tính từ tọa độ chuẩn của `campus`; 0 = trùng chỗ đặt chân cơ sở.
     * Mặc định 0 để chỗ dựng tin (repository/mock) tự tính qua [CampusReference],
     * không nhập tay — nguồn dựng tin phải đi qua một đường tính khoảng cách.
     */
    val distanceToCampusM: Int = 0,
    val status: HousingListingStatus = HousingListingStatus.OPEN,
    val verified: Boolean = false,
    val verificationMethod: HostVerificationMethod? = null,
    val areaM2: Int? = null,
    /** Viewer-relative: repository đặt true khi người đọc là chủ tin. */
    val isOwner: Boolean = false,
) {
    /** Tin chỉ hiện trên feed khi còn phòng và chưa bị kiểm duyệt ẩn. */
    val isPublishable: Boolean
        get() = status == HousingListingStatus.OPEN && availableSlots > 0

    /** Đã gắn vị trí khác 0/0 — feed/detail ẩn chip khoảng cách khi chưa gắn. */
    val hasLocation: Boolean
        get() = approxLatitude != 0.0 || approxLongitude != 0.0
}
