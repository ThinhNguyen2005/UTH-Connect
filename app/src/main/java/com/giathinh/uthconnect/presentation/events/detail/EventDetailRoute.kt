package com.giathinh.uthconnect.presentation.events.detail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun EventDetailRoute(
    eventId: String,
    onBack: () -> Unit,
    viewModel: EventDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    androidx.compose.runtime.LaunchedEffect(eventId) { viewModel.load(eventId) }
    EventDetailScreen(
        uiState = uiState,
        onAction = viewModel::onAction,
        onBack = onBack,
    )
}
