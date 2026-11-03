package com.giathinh.uthconnect.presentation.auth.forgotpassword

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun ForgotPasswordRoute(onBack: () -> Unit, modifier: Modifier = Modifier, viewModel: ForgotPasswordViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val back = { if (!state.isSending) { viewModel.onAction(ForgotPasswordAction.Clear); onBack() } }
    BackHandler(onBack = back)
    ForgotPasswordScreen(state, viewModel::onAction, back, modifier)
}
