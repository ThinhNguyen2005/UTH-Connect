package com.giathinh.uthconnect.presentation.lostfound

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

/** Màn con của tab Tiện ích; bottom bar tự ẩn theo destination hiện tại. */
@Serializable
data object LostFoundDestination

fun NavGraphBuilder.lostFoundGraph(navController: NavHostController) {
    composable<LostFoundDestination> {
        LostFoundRoute(onNavigateBack = { navController.popBackStack() })
    }
}
