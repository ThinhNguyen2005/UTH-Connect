package com.giathinh.uthconnect.presentation.auth

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.os.CancellationSignal
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import kotlinx.coroutines.flow.collect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.giathinh.uthconnect.R

/** Gate: platform biometric callback is required before granting a biometric demo session. */
@Composable
fun AuthRoute(
    signedInContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbars = remember { SnackbarHostState() }
    LaunchedEffect(viewModel, context) {
        viewModel.events.collect { event ->
            val resId = when (event) { AuthEvent.InvalidCredentials -> R.string.auth_invalid_credentials }
            snackbars.showSnackbar(context.getString(resId))
        }
    }
    when {
        state.isRestoring -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        state.student == null -> Box(modifier.fillMaxSize()) {
            AuthScreen(
            uiState = state,
            onAction = { action ->
                if (action == AuthAction.RequestBiometric) {
                    val activity = findAuthActivity(context)
                    if (activity == null || !state.biometricAvailable) {
                        viewModel.onAction(AuthAction.BiometricFailed)
                    } else {
                        showLoginBiometric(
                            activity,
                            onVerified = { viewModel.onAction(AuthAction.BiometricVerified) },
                            onError = { viewModel.onAction(AuthAction.BiometricFailed) },
                        )
                    }
                } else viewModel.onAction(action)
            },
            modifier = Modifier.fillMaxSize(),
        )
            SnackbarHost(hostState = snackbars, modifier = Modifier.align(Alignment.BottomCenter))
        }
        else -> signedInContent()
    }
}

private fun findAuthActivity(context: Context): Activity? {
    var current = context
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}

private fun showLoginBiometric(activity: Activity, onVerified: () -> Unit, onError: () -> Unit) {
    val strong = BiometricManager.Authenticators.BIOMETRIC_STRONG
    val manager = activity.getSystemService(BiometricManager::class.java)
    if (manager?.canAuthenticate(strong) != BiometricManager.BIOMETRIC_SUCCESS) {
        onError()
        return
    }
    val prompt = BiometricPrompt.Builder(activity)
        .setTitle(activity.getString(R.string.auth_biometric_login))
        .setSubtitle(activity.getString(R.string.auth_biometric_subtitle))
        .setAllowedAuthenticators(strong)
        .setNegativeButton(activity.getString(R.string.auth_cancel), activity.mainExecutor) { _, _ -> Unit }
        .build()
    prompt.authenticate(CancellationSignal(), activity.mainExecutor, object : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) { onVerified() }
        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) { onError() }
    })
}
