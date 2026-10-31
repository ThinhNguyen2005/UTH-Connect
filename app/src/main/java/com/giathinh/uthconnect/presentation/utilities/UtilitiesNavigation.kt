package com.giathinh.uthconnect.presentation.utilities

import kotlinx.serialization.Serializable

/**
 * Các route con bên trong module Tiện ích sinh viên.
 *
 * Định nghĩa dạng type-safe (@Serializable object) theo đúng kiến trúc của app
 * (docs/Design.md mục 8.1 và docs/CODING_STANDARDS.md mục 3.2.1).
 */
@Serializable
sealed interface UtilitiesDestination {

    /** Màn hình Hub chính: Lưới Grid icon các tiện ích sinh viên. */
    @Serializable
    data object Hub : UtilitiesDestination

    /** Màn hình Tra cứu học phí. */
    @Serializable
    data object Tuition : UtilitiesDestination

    /** Màn hình Tra cứu điểm rèn luyện. */
    @Serializable
    data object TrainingPoint : UtilitiesDestination

    /** Màn hình Thông tin các cơ sở UTH và Danh bạ phòng ban. */
    @Serializable
    data object Campuses : UtilitiesDestination

    /** Màn hình Cài đặt ứng dụng. */
    @Serializable
    data object Settings : UtilitiesDestination

    /** Màn hình Đồ thất lạc (Lost & Found). */
    @Serializable
    data object LostFound : UtilitiesDestination
}
