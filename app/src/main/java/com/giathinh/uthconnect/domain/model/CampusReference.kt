package com.giathinh.uthconnect.domain.model

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Tọa độ chuẩn của ba cơ sở UTH — gốc để tính `distanceToCampusM` theo
 * `README.md` mục 3.2 ("tính từ toạ độ chuẩn UTH").
 *
 * Tọa độ lấy theo điểm dễ nhận của từng cơ sở, đủ chính xác cho bộ lọc khoảng cách
 * (bán kính vài trăm mét); **không** dùng làm địa chỉ gửi xe hay dẫn đường chi tiết.
 * Khi trường công bố tọa độ chính thức thì chỉ đổi số trong object này, không đổi
 * công thức và không đổi call site.
 *
 * Khoảng cách tính bằng haversine thuần Kotlin: gọi API map để lấy khoảng cách vừa
 * tốn tiền vừa thêm dependency, trong khi nhu cầu ở đây chỉ là "gần/trường".
 */
object CampusReference {

    /** Bán kính Trái Đất dùng riêng cho công thức haversine bên dưới (mét). */
    private const val EARTH_RADIUS_M = 6_371_000.0

    /** Giới hạn vĩ/kinh độ hợp lệ — ngoài khoảng này là tọa độ rác. */
    private const val MAX_LATITUDE = 90.0
    private const val MAX_LONGITUDE = 180.0

    /**
     * Tọa độ chuẩn từng cơ sở (điểm dễ nhận, không phải địa chỉ dẫn đường chi tiết).
     * Theo node OpenStreetMap của từng cơ sở (11/2026): CS1 Võ Oanh (Bình Thạnh),
     * CS2 đường số 12 (An Khánh, Thủ Đức cũ), CS3 Tô Ký (Quận 12).
     */
    private val Cs1VoOanh = 10.8049 to 106.7168
    private val Cs2AnKhanh = 10.7921 to 106.7317
    private val Cs3ToKy = 10.8657 to 106.6187

    fun of(campus: UthCampus): Pair<Double, Double> = when (campus) {
        UthCampus.CS1_QUANG_TRUNG -> Cs1VoOanh
        UthCampus.CS2_THU_DUC -> Cs2AnKhanh
        UthCampus.CS3_TO_KY -> Cs3ToKy
    }

    /**
     * Khoảng cách đường chim bay (mét, làm tròn) từ một điểm tới cơ sở.
     * Trả `0` khi tọa độ ngoài khoảng hợp lệ, để UI hiển thị "ngay cơ sở" thay vì
     * một con số rác — thà nhầm nhẹ còn hơn in "cách ? m".
     */
    fun distanceMeters(campus: UthCampus, latitude: Double, longitude: Double): Int {
        val (campusLat, campusLng) = of(campus)
        if (!isUsable(latitude, longitude) || !isUsable(campusLat, campusLng)) return 0
        val dLat = Math.toRadians(campusLat - latitude)
        val dLng = Math.toRadians(campusLng - longitude)
        val a = sin(dLat / 2).pow(2.0) +
            cos(Math.toRadians(latitude)) * cos(Math.toRadians(campusLat)) * sin(dLng / 2).pow(2.0)
        return (EARTH_RADIUS_M * 2 * atan2(sqrt(a), sqrt(1 - a))).toInt()
    }

    /** Vĩ độ 0 là ngoài khơi vịnh Thái Lan, kinh độ 0 là gần London — cả hai đều là placeholder. */
    private fun isUsable(latitude: Double, longitude: Double): Boolean = latitude in -MAX_LATITUDE..MAX_LATITUDE && longitude in -MAX_LONGITUDE..MAX_LONGITUDE &&
        latitude != 0.0 && longitude != 0.0
}
