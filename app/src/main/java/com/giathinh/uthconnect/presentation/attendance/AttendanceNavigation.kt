package com.giathinh.uthconnect.presentation.attendance

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable data class AttendanceDestination(val eventId: String)

fun NavGraphBuilder.attendanceGraph(navController: NavHostController) {
    composable<AttendanceDestination> { entry -> AttendanceRoute(entry, navController) }
}
