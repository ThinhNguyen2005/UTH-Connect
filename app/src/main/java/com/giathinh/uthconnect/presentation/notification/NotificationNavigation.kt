package com.giathinh.uthconnect.presentation.notification

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.giathinh.uthconnect.presentation.navigation.ProfileDestination
import kotlinx.serialization.Serializable

/**
 * Điểm đến type-safe của màn hình Thông Báo (@Serializable object).
 */
@Serializable
object NotificationDestination

/** Type-safe detail destination used both by inbox taps and FCM notification taps. */
@Serializable
data class NotificationDetailDestination(val notificationId: String)

/**
 * Graph điều hướng của module Thông Báo.
 */
fun NavGraphBuilder.notificationGraph(navController: NavHostController) {
    composable<NotificationDestination> {
        NotificationRoute(
            onBack = { navController.popBackStack() },
            onOpenSettings = {
                // Điều hướng tới cài đặt (tab profile hoặc settings nếu có)
                navController.navigate(ProfileDestination)
            },
            onOpenProfile = {
                navController.navigate(ProfileDestination)
            },
            onOpenDetail = { notificationId -> navController.navigate(NotificationDetailDestination(notificationId)) },
        )
    }
    composable<NotificationDetailDestination> { entry ->
        val args = entry.toRoute<NotificationDetailDestination>()
        NotificationRoute(
            onBack = { navController.popBackStack() },
            onOpenSettings = { navController.navigate(ProfileDestination) },
            onOpenProfile = { navController.navigate(ProfileDestination) },
            onOpenDetail = { notificationId -> navController.navigate(NotificationDetailDestination(notificationId)) },
            notificationId = args.notificationId,
        )
    }
}
