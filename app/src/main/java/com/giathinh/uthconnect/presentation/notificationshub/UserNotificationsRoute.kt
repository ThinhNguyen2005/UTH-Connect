package com.giathinh.uthconnect.presentation.notificationshub

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun UserNotificationsRoute(
    onOpenHousing: () -> Unit,
    onOpenEvents: () -> Unit,
    onOpenIncidents: () -> Unit = {},
    viewModel: UserNotificationsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                UserNotificationsEvent.OpenHousing -> onOpenHousing()
                UserNotificationsEvent.OpenEvents -> onOpenEvents()
                UserNotificationsEvent.OpenIncidents -> onOpenIncidents()
            }
        }
    }
    UserNotificationsScreen(state = state, onAction = viewModel::onAction)
}
