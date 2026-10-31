package com.giathinh.uthconnect.presentation.utilities

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.giathinh.uthconnect.core.designsystem.theme.UthDimens
import com.giathinh.uthconnect.presentation.lostfound.LostFoundRoute
import com.giathinh.uthconnect.presentation.utilities.campuses.CampusesScreen
import com.giathinh.uthconnect.presentation.utilities.settings.SettingsScreen
import com.giathinh.uthconnect.presentation.utilities.trainingpoint.TrainingPointScreen
import com.giathinh.uthconnect.presentation.utilities.tuition.TuitionScreen

/**
 * Tầng nối (route) của tab Tiện ích: Quản lý ViewModel và Navigation con
 * giữa Hub Tiện ích với các màn hình con tương ứng.
 */
@Composable
fun UtilitiesRoute(
    modifier: Modifier = Modifier,
    viewModel: UtilitiesViewModel = hiltViewModel(),
    navController: NavHostController = rememberNavController(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val tuitionState by viewModel.tuitionState.collectAsStateWithLifecycle()
    val trainingPointState by viewModel.trainingPointState.collectAsStateWithLifecycle()
    val settingsState by viewModel.settingsState.collectAsStateWithLifecycle()

    NavHost(
        navController = navController,
        startDestination = UtilitiesDestination.Hub,
        modifier = modifier,
        enterTransition = { fadeIn(tween(UthDimens.Motion.RouteFadeMillis)) },
        exitTransition = { fadeOut(tween(UthDimens.Motion.RouteFadeMillis)) },
    ) {
        composable<UtilitiesDestination.Hub> {
            UtilitiesScreen(
                uiState = uiState,
                onAction = viewModel::onAction,
                onNavigateToTuition = { navController.navigate(UtilitiesDestination.Tuition) },
                onNavigateToTrainingPoint = { navController.navigate(UtilitiesDestination.TrainingPoint) },
                onNavigateToCampuses = { navController.navigate(UtilitiesDestination.Campuses) },
                onNavigateToSettings = { navController.navigate(UtilitiesDestination.Settings) },
                onNavigateToLostFound = { navController.navigate(UtilitiesDestination.LostFound) },
            )
        }

        composable<UtilitiesDestination.Tuition> {
            TuitionScreen(
                uiState = tuitionState,
                onBack = { navController.popBackStack() },
            )
        }

        composable<UtilitiesDestination.TrainingPoint> {
            TrainingPointScreen(
                uiState = trainingPointState,
                onBack = { navController.popBackStack() },
            )
        }

        composable<UtilitiesDestination.Campuses> {
            CampusesScreen(
                onBack = { navController.popBackStack() },
            )
        }

        composable<UtilitiesDestination.Settings> {
            SettingsScreen(
                uiState = settingsState,
                onToggleTuitionReminder = viewModel::onToggleTuitionReminder,
                onToggleTrainingPointNotif = viewModel::onToggleTrainingPointNotif,
                onBack = { navController.popBackStack() },
            )
        }

        composable<UtilitiesDestination.LostFound> {
            LostFoundRoute(
                onNavigateBack = { navController.popBackStack() },
            )
        }
    }
}
