package com.giathinh.uthconnect.presentation.profile

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.os.CancellationSignal
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import kotlinx.coroutines.flow.collect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.giathinh.uthconnect.R

@Composable
fun ProfileRoute(
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbars = remember { SnackbarHostState() }
    LaunchedEffect(viewModel, context) {
        viewModel.events.collect { event ->
            val resId = when (event) {
                ProfileEvent.LogoutFailed -> R.string.auth_logout_failed
                ProfileEvent.PasswordChanged -> R.string.auth_password_changed
                ProfileEvent.ContactSaved -> R.string.auth_contact_saved
            }
            snackbars.showSnackbar(context.getString(resId))
        }
    }
    BackHandler(enabled = uiState.page != ProfilePage.Overview || uiState.confirmLogout) {
        if (uiState.confirmLogout) viewModel.onAction(ProfileAction.CancelLogout)
        else viewModel.onAction(ProfileAction.Back)
    }
    Box(modifier.fillMaxSize()) {
        ProfileScreen(uiState, onAction = { action ->
        if (action == ProfileAction.RequestBiometricEnroll) {
            val activity = findProfileActivity(context)
            if (activity == null || uiState.biometricEnabled) {
                viewModel.onAction(ProfileAction.BiometricFailed)
            } else {
                showEnrollmentPrompt(activity,
                    onVerified = { viewModel.onAction(ProfileAction.BiometricVerified) },
                    onError = { viewModel.onAction(ProfileAction.BiometricFailed) })
            }
        } else viewModel.onAction(action)
    }, modifier = Modifier.fillMaxSize())
        SnackbarHost(hostState = snackbars, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

private fun findProfileActivity(context: Context): Activity? {
    var current = context
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}

private fun showEnrollmentPrompt(activity: Activity, onVerified: () -> Unit, onError: () -> Unit) {
    val strong = BiometricManager.Authenticators.BIOMETRIC_STRONG
    val manager = activity.getSystemService(BiometricManager::class.java)
    if (manager?.canAuthenticate(strong) != BiometricManager.BIOMETRIC_SUCCESS) {
        onError()
        return
    }
    val prompt = BiometricPrompt.Builder(activity)
        .setTitle(activity.getString(R.string.auth_biometric_enable))
        .setSubtitle(activity.getString(R.string.auth_biometric_subtitle))
        .setAllowedAuthenticators(strong)
        .setNegativeButton(activity.getString(R.string.auth_cancel), activity.mainExecutor) { _, _ -> Unit }
        .build()
    prompt.authenticate(CancellationSignal(), activity.mainExecutor, object : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) { onVerified() }
        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) { onError() }
    })
}
