package com.giathinh.uthconnect.presentation.profile

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.os.CancellationSignal
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
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
    val lifecycleOwner = LocalLifecycleOwner.current
    val biometric = remember(context) { findProfileActivity(context)?.let { ProfileBiometricController(it) } }
    DisposableEffect(lifecycleOwner, biometric) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) biometric?.cancel()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer); biometric?.cancel() }
    }
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
        if (uiState.isSaving || uiState.isLoggingOut) return@BackHandler
        if (uiState.confirmLogout) viewModel.onAction(ProfileAction.CancelLogout)
        else viewModel.onAction(ProfileAction.Back)
    }
    // Dành vùng riêng để Snackbar không che nút đăng xuất ở cuối nội dung.
    Column(modifier.fillMaxSize()) {
        ProfileScreen(uiState, onAction = { action ->
        if (action == ProfileAction.RequestBiometricEnroll) {
            val activity = findProfileActivity(context)
            if (activity == null || uiState.biometricEnabled) {
                viewModel.onAction(ProfileAction.BiometricFailed)
            } else {
                (biometric ?: return@ProfileScreen).show(
                    onVerified = { viewModel.onAction(ProfileAction.BiometricVerified) },
                    onError = { viewModel.onAction(ProfileAction.BiometricFailed) })
            }
        } else viewModel.onAction(action)
    }, modifier = Modifier.weight(1f))
        SnackbarHost(hostState = snackbars)
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

/** Một prompt mỗi Route; bỏ callback đến sau khi stop/dispose. */
private class ProfileBiometricController(private val activity: Activity) {
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
                .setTitle(activity.getString(R.string.auth_biometric_enable))
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
