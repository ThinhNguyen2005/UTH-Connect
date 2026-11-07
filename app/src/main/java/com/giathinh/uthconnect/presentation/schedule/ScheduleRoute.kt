package com.giathinh.uthconnect.presentation.schedule

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay

/** Connects the schedule state holder and polls remote events while the route is visible. */
@Composable
fun ScheduleRoute(viewModel: ScheduleViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(viewModel, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                delay(EVENT_REFRESH_INTERVAL_MILLIS)
                viewModel.onAction(ScheduleAction.PollEvents)
            }
        }
    }
    ScheduleScreen(state, viewModel::onAction)
}

private const val EVENT_REFRESH_INTERVAL_MILLIS = 15_000L
