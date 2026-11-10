package com.giathinh.uthconnect.presentation.utilities

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.giathinh.uthconnect.core.designsystem.theme.UthDimens
import com.giathinh.uthconnect.domain.model.UtilityModuleId
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
    onOpenModule: (UtilityModuleId) -> Unit = {},
    viewModel: UtilitiesViewModel = hiltViewModel(),
    navController: NavHostController = rememberNavController(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val tuitionState by viewModel.tuitionState.collectAsStateWithLifecycle()
    val trainingPointState by viewModel.trainingPointState.collectAsStateWithLifecycle()
    val settingsState by viewModel.settingsState.collectAsStateWithLifecycle()

    // Đổi ngôn ngữ chỉ có hiệu lực khi activity dựng lại tài nguyên: Locale được áp
    // trong `MainActivity.attachBaseContext`, chạy trước mọi Composable.
    val context = LocalContext.current
    LaunchedEffect(viewModel, context) {
        viewModel.events.collect { event ->
            when (event) {
                UtilitiesEvent.LanguageChanged -> activityOf(context)?.recreate()
            }
        }
    }

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
                onAction = { action ->
                    if (action is UtilitiesAction.OpenModule) {
                        if (uiState.modules.any { it.id == action.moduleId && it.enabled }) {
                            onOpenModule(action.moduleId)
                        }
                    } else {
                        viewModel.onAction(action)
                    }
                },
                onNavigateToTuition = { navController.navigate(UtilitiesDestination.Tuition) },
                onNavigateToTrainingPoint = { navController.navigate(UtilitiesDestination.TrainingPoint) },
                onNavigateToCampuses = { navController.navigate(UtilitiesDestination.Campuses) },
                onNavigateToSettings = { navController.navigate(UtilitiesDestination.Settings) },
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
                onToggleTuitionReminder = { viewModel.onAction(UtilitiesAction.TuitionReminderChanged(it)) },
                onToggleTrainingPointNotif = { viewModel.onAction(UtilitiesAction.TrainingPointNotifChanged(it)) },
                onSelectLanguage = { viewModel.onAction(UtilitiesAction.LanguageSelected(it)) },
                onSelectThemeMode = { viewModel.onAction(UtilitiesAction.ThemeModeSelected(it)) },
                onBack = { navController.popBackStack() },
            )
        }
    }
}

/**
 * `LocalContext` trong `NavHost` là `ContextThemeWrapper`, cast thẳng ra Activity không
 * được — phải đi ngược chuỗi `baseContext` (cùng cách `AuthRoute` đọc intent deep link).
 */
private fun activityOf(context: Context): Activity? {
    var current = context
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}
