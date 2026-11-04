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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import kotlinx.coroutines.flow.collect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.giathinh.uthconnect.BuildConfig
import com.giathinh.uthconnect.R
import com.giathinh.uthconnect.data.repository.GoogleIdTokenResult
import com.giathinh.uthconnect.data.repository.GoogleIdTokenSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** Gate: platform biometric callback is required before granting a biometric demo session. */
@Composable
fun AuthRoute(
    signedInContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scopes = rememberCoroutineScope()
    val googleIdTokens = rememberGoogleIdTokenSource()
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
                when (action) {
                    AuthAction.RequestBiometric -> {
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
                    }
                    AuthAction.RequestGoogleSignIn -> launchGoogleSignIn(
                        activity = findAuthActivity(context),
                        source = googleIdTokens,
                        scopes = scopes,
                        viewModel = viewModel,
                    )
                    else -> viewModel.onAction(action)
                }
            },
            modifier = Modifier.fillMaxSize(),
        )
            SnackbarHost(hostState = snackbars, modifier = Modifier.align(Alignment.BottomCenter))
        }
        else -> signedInContent()
    }
}

/**
 * Google's account picker is platform UI, so Credential Manager runs here and reports back as
 * actions — the ViewModel only ever sees a token, a cancellation, or an unavailable device.
 */
private fun launchGoogleSignIn(
    activity: Activity?,
    source: GoogleIdTokenSource,
    scopes: CoroutineScope,
    viewModel: AuthViewModel,
) {
    // Marks the button busy first: every early return below has to clear it again.
    viewModel.onAction(AuthAction.RequestGoogleSignIn)
    if (activity == null) {
        viewModel.onAction(AuthAction.GoogleSignInUnavailable)
        return
    }
    scopes.launch {
        when (val result = source.request(activity, BuildConfig.GOOGLE_WEB_CLIENT_ID)) {
            is GoogleIdTokenResult.Granted -> viewModel.onAction(
                AuthAction.GoogleIdTokenReceived(result.idToken, result.rawNonce),
            )
            GoogleIdTokenResult.Cancelled -> viewModel.onAction(AuthAction.GoogleSignInCancelled)
            GoogleIdTokenResult.Unavailable -> viewModel.onAction(AuthAction.GoogleSignInUnavailable)
        }
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
