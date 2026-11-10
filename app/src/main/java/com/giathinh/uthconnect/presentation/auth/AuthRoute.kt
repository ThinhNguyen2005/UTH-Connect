package com.giathinh.uthconnect.presentation.auth

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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

/** Gate: validates student session before displaying signed-in content. */
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
            when (event) {
                AuthEvent.InvalidCredentials ->
                    snackbars.showSnackbar(context.getString(R.string.auth_invalid_credentials))

                is AuthEvent.GeneralMessage -> snackbars.showSnackbar(event.message)

                // Locale áp ở MainActivity.attachBaseContext → phải dựng lại activity,
                // không có thông điệp snackbar cho event này.
                AuthEvent.LanguageChanged -> findAuthActivity(context)?.recreate()
            }
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
