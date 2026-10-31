package com.giathinh.uthconnect.presentation.announcement

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

/**
 * Route con của module Thông báo: màn hình Chi tiết, tham số là id thông báo.
 *
 * Khai báo ở đây chứ không cộng vào enum 4 tab (`UthDestination.kt`): enum đó chỉ giữ route cấp 1
 * có mặt trên bottom navigation.
 */
@Serializable
data class AnnouncementDetailDestination(val announcementId: String)

/**
 * Graph con của module Thông báo.
 *
 * `UthApp.kt` chỉ gọi đúng một dòng `announcementGraph(navController)` và một dòng điều hướng từ
 * bảng tin — phần còn lại của việc nối route nằm trong module này (`docs/REPO_TREE.md` mục 5).
 */
fun NavGraphBuilder.announcementGraph(navController: NavHostController) {
    composable<AnnouncementDetailDestination> {
        AnnouncementRoute(onBack = { navController.popBackStack() })
    }
}
