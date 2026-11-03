package com.giathinh.uthconnect.presentation.auth

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.os.CancellationSignal
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CustomCredential
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.CancellationException
import java.security.SecureRandom
import java.util.Base64
import com.giathinh.uthconnect.presentation.auth.verifyemail.VerifyEmailRoute
import com.giathinh.uthconnect.presentation.auth.forgotpassword.ForgotPasswordRoute
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
    verifyContent: @Composable (Boolean) -> Unit = { VerifyEmailRoute(remember = it, modifier = modifier) },
    forgotContent: @Composable (() -> Unit) -> Unit = { ForgotPasswordRoute(onBack = it, modifier = modifier) },
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
            when (event) {
                AuthEvent.InvalidCredentials -> snackbars.showSnackbar(context.getString(R.string.auth_invalid_credentials))
                AuthEvent.RequestGoogle -> {
                    try {
                        val clientIdRes = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
                        if (clientIdRes == 0) { viewModel.onAction(AuthAction.GoogleFailed(configuration = true)); return@collect }
                        val nonce = Base64.getUrlEncoder().withoutPadding().encodeToString(ByteArray(32).also { SecureRandom().nextBytes(it) })
                        val option = GetSignInWithGoogleOption.Builder(context.getString(clientIdRes)).setNonce(nonce).build()
                        val response = CredentialManager.create(context).getCredential(context,
                            GetCredentialRequest.Builder().addCredentialOption(option).build())
                        val credential = response.credential
                        if (credential !is CustomCredential || credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                            viewModel.onAction(AuthAction.GoogleFailed())
                        } else {
                            val token = GoogleIdTokenCredential.createFrom(credential.data).idToken
                            val payload = org.json.JSONObject(String(Base64.getUrlDecoder().decode(token.split('.')[1]), Charsets.UTF_8))
                            if (payload.optString("nonce") != nonce) viewModel.onAction(AuthAction.GoogleFailed())
                            else viewModel.onAction(AuthAction.GoogleTokenReceived(token))
                        }
                    } catch (_: GetCredentialCancellationException) { viewModel.onAction(AuthAction.GoogleCancelled) }
                    catch (cancelled: CancellationException) { viewModel.onAction(AuthAction.GoogleCancelled); throw cancelled }
                    catch (_: Exception) { viewModel.onAction(AuthAction.GoogleFailed()) }
                }
                AuthEvent.ClearCredentials -> {
                    try { CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest()) }
                    catch (cancelled: CancellationException) { throw cancelled }
                    catch (_: Exception) { /* Firebase đã signOut; không cấp lại phiên nếu provider không hỗ trợ clear. */ }
                }
            }
        }
    }
    when {
        state.isRestoring -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        state.pendingIdentity != null -> verifyContent(state.rememberMe)
        state.forgotDialogOpen -> forgotContent { viewModel.onAction(AuthAction.DismissForgotPassword) }
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
