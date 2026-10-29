package com.giathinh.uthconnect.domain.model

/**
 * Tiến độ giao module theo `docs/Design.md` mục 3. Domain chỉ mang tính định tuyến
 * UI (nhãn + màu badge), không quyết định kỹ thuật.
 */
enum class DeliveryPhase {
    /** MVP — làm và demo được ngay trong học kỳ doanh nghiệp. */
    MVP,

    /** Phase 2 — sau khi MVP chạy thật. */
    PHASE_2,

    /** Phase 3 */
    PHASE_3,

    /** Backlog — chỉ làm khi các hạng mục ưu tiên đã ổn định. */
    BACKLOG,
}

/** Định danh các module của UTH Connect. */
enum class UtilityModuleId {
    EVENTS,
    TIMETABLE,
    LOST_FOUND,
    HOUSING,
    RIDE_SHARE,
    BOOKS,
}

/**
 * Một module mà sinh viên nhìn thấy trên tab Tiện ích.
 * `enabled = false` nghĩa màn hình chi tiết chưa làm → UI hiện badge giai đoạn
 * và không điều hướng vào trong.
 */
data class UtilityModule(
    val id: UtilityModuleId,
    val phase: DeliveryPhase,
    val enabled: Boolean,
)
