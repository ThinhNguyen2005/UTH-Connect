package com.giathinh.uthconnect.domain.model

/*
 * Mô hình Ghép trọ theo `README.md` mục 3.2: tin phòng/ở ghép kèm giá, khoảng cách
 * tới cơ sở và nhu cầu bạn ở ghép.
 *
 * Tọa độ là dữ liệu, không phải chuỗi: `latitude`/`longitude` nuôi bộ lọc
 * "khoảng cách tới cơ sở" còn `addressLabel` chỉ để hiển thị. Từ 11/11/2026 địa chỉ
 * và SĐT/Zalo hiển thị công khai cho người đã đăng nhập — bước duyệt liên hệ đã bỏ
 * (quyết định sản phẩm: app chỉ nhận sinh viên UTH đã xác thực).
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

/**
 * Cơ sở UTH dùng làm gốc tính khoảng cách. Tên enum là giá trị lịch sử trong cột
 * `campus` của DB (CS1 từng bị gọi nhầm "Quang Trung") — giữ nguyên để không phải
 * migration dữ liệu; nhãn hiển thị đúng nằm ở `housing_campus_cs*` (strings.xml).
 */
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
    /** Số điện thoại gọi trực tiếp (Jira). */
    val contactPhone: String = "",
    /** Đường dẫn Zalo (zalo.me/...) mở trực tiếp (Jira). */
    val contactZaloUrl: String = "",
    /**
     * `false` = chủ tin chủ động tắt liên hệ Zalo — detail ẩn nút Nhắn Zalo và
     * không fallback SĐT. Tin đời cũ chưa có cột này mặc định `true` (URL rỗng
     * vẫn fallback SĐT theo quy tắc module).
     */
    val contactZaloEnabled: Boolean = true,
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
    val latitude: Double,
    val longitude: Double,
    /**
     * Mét tới `campus`: ưu tiên đường đi thật (OSRM), fallback đường thẳng
     * haversine qua [CampusReference]; 0 = tin chưa gắn vị trí thật.
     * Mặc định 0 để chỗ dựng tin (repository/mock) tự tính, không nhập tay.
     */
    val distanceToCampusM: Int = 0,
    val status: HousingListingStatus = HousingListingStatus.OPEN,
    val verified: Boolean = false,
    val verificationMethod: HostVerificationMethod? = null,
    val areaM2: Int? = null,
    /** ISO-8601 từ `created_at`; rỗng = nguồn không có (mock) → UI ẩn nhãn giờ đăng. */
    val createdAt: String = "",
) {
    /** Tin chỉ hiện trên feed khi còn phòng và chưa bị kiểm duyệt ẩn. */
    val isPublishable: Boolean
        get() = status == HousingListingStatus.OPEN && availableSlots > 0
}
