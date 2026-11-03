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
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
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

/** Chỉ mở nội dung sau khi phiên Firebase và cổng sinh trắc học đã hợp lệ. */
@Composable
fun AuthRoute(
    signedInContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val biometric = remember(context) { findAuthActivity(context)?.let { AuthBiometricController(it) } }
    DisposableEffect(lifecycleOwner, biometric) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) biometric?.cancel()
            if (event == Lifecycle.Event.ON_START) viewModel.onAction(AuthAction.RefreshSession)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer); biometric?.cancel() }
    }
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
                        (biometric ?: return@AuthScreen).show(
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

/** Một prompt mỗi Route; bỏ callback đến sau khi stop/dispose. */
private class AuthBiometricController(private val activity: Activity) {
    private var signal: CancellationSignal? = null
    fun cancel() {
        val pending = signal
        signal = null
        pending?.cancel()
    }
    fun show(onVerified: () -> Unit, onError: () -> Unit) {
        if (signal != null) return
        val strong = BiometricManager.Authenticators.BIOMETRIC_STRONG
        val manager = activity.getSystemService(BiometricManager::class.java)
        if (manager?.canAuthenticate(strong) != BiometricManager.BIOMETRIC_SUCCESS) { onError(); return }
        val pending = CancellationSignal()
        signal = pending
        try {
            val prompt = BiometricPrompt.Builder(activity)
                .setTitle(activity.getString(R.string.auth_biometric_login))
                .setSubtitle(activity.getString(R.string.auth_biometric_subtitle))
                .setAllowedAuthenticators(strong)
                .setNegativeButton(activity.getString(R.string.auth_cancel), activity.mainExecutor) { _, _ -> cancel() }
                .build()
            prompt.authenticate(pending, activity.mainExecutor, object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    if (signal !== pending) return
                    signal = null
                    onVerified()
                }
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    if (signal !== pending) return
                    signal = null
                    if (errorCode != BiometricPrompt.BIOMETRIC_ERROR_USER_CANCELED &&
                        errorCode != BiometricPrompt.BIOMETRIC_ERROR_CANCELED) onError()
                }
            })
        } catch (_: Exception) { cancel(); onError() }
    }
}
