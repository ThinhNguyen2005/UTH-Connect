package com.giathinh.uthconnect.presentation.profile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/** Chỉ Route biết Hilt và ViewModel; Screen nhận trạng thái đơn thuần. */
@Composable
fun ProfileRoute(
    modifier: Modifier = Modifier,
    onOpenUtilities: () -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ProfileScreen(
        uiState = uiState,
        onAction = viewModel::onAction,
        onOpenUtilities = onOpenUtilities,
        modifier = modifier,
    )
}
