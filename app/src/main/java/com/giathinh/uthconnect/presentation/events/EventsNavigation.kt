package com.giathinh.uthconnect.presentation.events

import androidx.compose.runtime.Composable
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.giathinh.uthconnect.presentation.attendance.AttendanceDestination
import kotlinx.serialization.Serializable

/**
 * Sự kiện không còn là tab đáy (khung app giữ 4 tab — `docs/Design.md` mục 8.1), nên route
 * của nó thuộc về module này thay vì nằm chung file với enum tab.
 * Điểm vào thực tế: ô "Sự kiện & CLB" trong lưới tab Tiện ích.
 */
@Serializable data object EventsDestination

fun NavGraphBuilder.eventsGraph(navController: NavHostController) {
    composable<EventsDestination> {
        EventsRoute(onOpenAttendance = { eventId -> navController.navigate(AttendanceDestination(eventId)) })
    }
}
