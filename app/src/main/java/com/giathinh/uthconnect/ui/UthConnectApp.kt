package com.giathinh.uthconnect.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.giathinh.uthconnect.feature.auth.AuthViewModel
import com.giathinh.uthconnect.feature.auth.Destination
import com.giathinh.uthconnect.feature.auth.ui.LoginScreen
import com.giathinh.uthconnect.feature.home.ui.HomeScreen
import com.giathinh.uthconnect.feature.profile.ui.ProfileScreen

/** Single-activity navigation for the exact Login -> Home -> Profile scope. */
@Composable
fun UthConnectApp(viewModel: AuthViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    // Snapshot an ordinary local val so Kotlin can safely smart-cast after null check.
    val student = state.student
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        when {
            state.isRestoring -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            student == null -> LoginScreen(
                state = state,
                onIdentifierChange = viewModel::onIdentifierChanged,
                onPasswordChange = viewModel::onPasswordChanged,
                onRememberMeChange = viewModel::onRememberMeChanged,
                onPasswordVisibilityClick = viewModel::togglePasswordVisibility,
                onForgotPasswordClick = { viewModel.setForgotPasswordDialog(true) },
                onForgotPasswordDismiss = { viewModel.setForgotPasswordDialog(false) },
                onSignInClick = viewModel::signIn,
            )
            state.destination == Destination.PROFILE -> {
                BackHandler { viewModel.backHome() }
                ProfileScreen(
                    student = student,
                    onBack = viewModel::backHome,
                    onSignOut = viewModel::signOut,
                )
            }
            else -> HomeScreen(student = student, onOpenProfile = viewModel::openProfile)
        }
    }
}
