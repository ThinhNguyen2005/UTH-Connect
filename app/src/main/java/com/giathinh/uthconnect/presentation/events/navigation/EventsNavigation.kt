package com.giathinh.uthconnect.presentation.events.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import androidx.navigation.toRoute
import com.giathinh.uthconnect.presentation.events.EventsRoute
import com.giathinh.uthconnect.presentation.events.detail.EventDetailScreen
import kotlinx.serialization.Serializable

@Serializable object EventsGraph
@Serializable object EventsDestination
@Serializable data class EventDetailDestination(val eventId: String)

/** Graph con thuộc tab Tiện ích. Chỉ truyền ID qua route, không truyền cả Event. */
fun NavGraphBuilder.eventsGraph(navController: NavController) {
    navigation<EventsGraph>(startDestination = EventsDestination) {
        composable<EventsDestination> {
            EventsRoute(
                onOpenDetail = { id -> navController.navigate(EventDetailDestination(id)) { launchSingleTop = true } },
                onBack = { navController.popBackStack() },
            )
        }
        composable<EventDetailDestination> { entry ->
            val destination = entry.toRoute<EventDetailDestination>()
            EventDetailScreen(eventId = destination.eventId, onBack = { navController.popBackStack() })
        }
    }
}
