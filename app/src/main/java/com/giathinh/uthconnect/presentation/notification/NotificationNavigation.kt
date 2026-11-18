package com.giathinh.uthconnect.presentation.notification

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.giathinh.uthconnect.presentation.events.EventSourceDestination
import com.giathinh.uthconnect.presentation.housing.HousingDetailDestination
import com.giathinh.uthconnect.presentation.navigation.ProfileDestination
import com.giathinh.uthconnect.presentation.navigation.SettingsDestination
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
fun NavGraphBuilder.notificationGraph(
    navController: NavHostController,
    onInboxRead: () -> Unit = {},
) {
    composable<NotificationDestination> {
        NotificationRoute(
            onBack = { navController.popBackStack() },
            onOpenSettings = {
                navController.navigate(SettingsDestination)
            },
            onOpenProfile = {
                navController.navigate(ProfileDestination)
            },
            onOpenDetail = { notificationId -> navController.navigate(NotificationDetailDestination(notificationId)) },
            onInboxRead = onInboxRead,
            onOpenSource = { type, id -> navController.openNotificationSource(type, id) },
        )
    }
    composable<NotificationDetailDestination> { entry ->
        val args = entry.toRoute<NotificationDetailDestination>()
        NotificationRoute(
            onBack = { navController.popBackStack() },
            onOpenSettings = { navController.navigate(SettingsDestination) },
            onOpenProfile = { navController.navigate(ProfileDestination) },
            onOpenDetail = { notificationId -> navController.navigate(NotificationDetailDestination(notificationId)) },
            onInboxRead = onInboxRead,
            notificationId = args.notificationId,
            onOpenSource = { type, id -> navController.openNotificationSource(type, id) },
        )
    }
}

private fun NavHostController.openNotificationSource(type: String, id: String) {
    when (type) {
        "EVENT" -> navigate(EventSourceDestination(id))
        "HOUSING" -> navigate(HousingDetailDestination(listingId = id))
    }
}
