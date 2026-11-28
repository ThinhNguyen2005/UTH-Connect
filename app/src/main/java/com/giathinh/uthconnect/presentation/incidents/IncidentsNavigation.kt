@file:Suppress("MatchingDeclarationName")

package com.giathinh.uthconnect.presentation.incidents

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable
data object IncidentsDestination

fun NavGraphBuilder.incidentsGraph(navController: NavHostController) {
    composable<IncidentsDestination> {
        IncidentsRoute(onNavigateBack = { navController.popBackStack() })
    }
}
