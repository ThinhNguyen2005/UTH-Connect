package com.giathinh.uthconnect.presentation.auth

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/** Gate chạy ở root: chưa đăng nhập -> Login; đã đăng nhập -> 4 tab của nhóm. */
@Composable
fun AuthRoute(
    signedInContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    when {
        state.isRestoring -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        state.student == null -> AuthScreen(
            uiState = state,
            onAction = viewModel::onAction,
            modifier = modifier,
        )
        else -> signedInContent()
    }
}
