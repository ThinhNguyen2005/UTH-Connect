package com.giathinh.uthconnect.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.giathinh.uthconnect.BuildConfig
import com.giathinh.uthconnect.domain.usecase.AccessStudentSessionUseCase
import com.giathinh.uthconnect.domain.usecase.SignInResult
import com.giathinh.uthconnect.domain.usecase.SignInWithGoogleResult
import com.giathinh.uthconnect.domain.usecase.SignInWithGoogleUseCase
import com.giathinh.uthconnect.domain.usecase.SignInUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val signIn: SignInUseCase,
    private val sessions: AccessStudentSessionUseCase,
    private val signInWithGoogle: SignInWithGoogleUseCase,
) : ViewModel() {
    private val googleEnabled = BuildConfig.USE_SUPABASE && BuildConfig.GOOGLE_WEB_CLIENT_ID.isNotBlank()
    private val _uiState = MutableStateFlow(AuthUiState(googleSignInAvailable = googleEnabled))
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()
    private val eventChannel = Channel<AuthEvent>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()

    init {
        viewModelScope.launch {
            try { sessions.restore() }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { _uiState.update { it.copy(isRestoring = false, failure = AuthFailure.SessionRestore) } }
            sessions.observe().collect { current ->
                _uiState.update {
                    val justLoggedOut = it.student != null && current.profile == null
                    val saved = current.profile ?: it.savedStudent
                    it.copy(
                        isRestoring = current.isRestoring,
                        student = current.profile,
                        savedStudent = saved,
                        identifier = if (justLoggedOut) "" else it.identifier,
                        password = if (justLoggedOut) "" else it.password,
                        biometricAvailable = current.biometricStudentId != null,
                        rememberMe = current.remembered,
                        failure = if (current.failure != null) AuthFailure.SessionRestore else it.failure,
                    )
                }
            }
        }
    }

    fun onAction(action: AuthAction) {
        when (action) {
            is AuthAction.SelectMode -> _uiState.update { it.copy(authMode = action.mode, failure = null) }
            is AuthAction.SelectLanguage -> _uiState.update { it.copy(selectedLanguage = action.lang) }
            AuthAction.SwitchAccount -> _uiState.update {
                it.copy(
                    authMode = AuthMode.Password,
                    identifier = "",
                    password = "",
                    failure = null,
                )
            }
            is AuthAction.IdentifierChanged -> _uiState.update {
                it.copy(identifier = action.value, identifierError = null, failure = null)
            }
            is AuthAction.PasswordChanged -> _uiState.update {
                it.copy(password = action.value, passwordError = null, failure = null)
            }
            is AuthAction.RememberChanged -> _uiState.update { it.copy(rememberMe = action.value) }
            AuthAction.TogglePassword -> _uiState.update { it.copy(passwordVisible = !it.passwordVisible) }
            AuthAction.ForgotPassword -> _uiState.update { it.copy(forgotDialogOpen = true) }
            AuthAction.DismissForgotPassword -> _uiState.update { it.copy(forgotDialogOpen = false) }
            AuthAction.Submit, AuthAction.Retry -> submit()
            AuthAction.RequestBiometric -> Unit // platform prompt belongs to AuthRoute
            AuthAction.BiometricVerified -> biometricUnlock()
            AuthAction.BiometricFailed -> _uiState.update {
                it.copy(isSigningIn = false, failure = AuthFailure.BiometricUnavailable)
            }
            // Credential Manager itself runs in AuthRoute; these are its callbacks only.
            AuthAction.RequestGoogleSignIn -> _uiState.update {
                if (googleEnabled && it.student == null) it.copy(isSigningInWithGoogle = true, failure = null) else it
            }
            is AuthAction.GoogleIdTokenReceived -> googleSignIn(action.idToken, action.rawNonce)
            AuthAction.GoogleSignInCancelled -> _uiState.update { it.copy(isSigningInWithGoogle = false) }
            AuthAction.GoogleSignInUnavailable -> _uiState.update {
                it.copy(isSigningInWithGoogle = false, failure = AuthFailure.GoogleUnavailable)
            }
        }
    }

    /**
     * The Google button has no form validation to fail, so the only states are: signed in,
     * waiting for MSSV verification, or rejected. Verification pending keeps the user on this
     * screen with an explanation instead of a database error later.
     */
    private fun googleSignIn(idToken: String, rawNonce: String) {
        val snapshot = _uiState.value
        // isSigningInWithGoogle is already true: AuthRoute raised the OS picker first.
        if (!googleEnabled || snapshot.isSigningIn || snapshot.student != null) return
        _uiState.update {
            it.copy(isSigningInWithGoogle = true, failure = null, googleVerification = null)
        }
        viewModelScope.launch {
            try {
                when (val outcome = signInWithGoogle(idToken, rawNonce)) {
                    is SignInWithGoogleResult.Success -> _uiState.update {
                        it.copy(isSigningInWithGoogle = false, identifier = "", password = "")
                    }
                    is SignInWithGoogleResult.VerificationPending -> _uiState.update {
                        it.copy(
                            isSigningInWithGoogle = false,
                            googleVerification = GoogleVerificationNotice(outcome.email, outcome.displayName),
                        )
                    }
                    SignInWithGoogleResult.Rejected, SignInWithGoogleResult.Unsupported -> _uiState.update {
                        it.copy(isSigningInWithGoogle = false, failure = AuthFailure.GoogleRejected)
                    }
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) {
                _uiState.update { it.copy(isSigningInWithGoogle = false, failure = AuthFailure.GoogleRejected) }
            }
        }
    }

    private fun biometricUnlock() {
        if (_uiState.value.isSigningIn || !_uiState.value.biometricAvailable) return
        _uiState.update { it.copy(isSigningIn = true, failure = null) }
        viewModelScope.launch {
            try { sessions.unlockAfterBiometricPrompt() }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { _uiState.update { it.copy(failure = AuthFailure.BiometricUnavailable) } }
            finally { _uiState.update { it.copy(isSigningIn = false) } }
        }
    }

    private fun submit() {
        val snapshot = _uiState.value
        if (snapshot.isRestoring || snapshot.isSigningIn || snapshot.student != null) return
        _uiState.update { it.copy(isSigningIn = true, failure = null) }
        viewModelScope.launch {
            try {
                when (val outcome = signIn(snapshot.identifier, snapshot.password, snapshot.rememberMe)) {
                    is SignInResult.Success -> _uiState.update {
                        it.copy(isSigningIn = false, password = "", identifierError = null, passwordError = null)
                    }
                    is SignInResult.InvalidForm -> _uiState.update {
                        it.copy(isSigningIn = false, identifierError = outcome.identifier, passwordError = outcome.password)
                    }
                    SignInResult.InvalidCredentials -> {
                        _uiState.update { it.copy(isSigningIn = false, failure = AuthFailure.InvalidCredentials) }
                        eventChannel.send(AuthEvent.InvalidCredentials)
                    }
                    SignInResult.Failure -> _uiState.update {
                        it.copy(isSigningIn = false, failure = AuthFailure.Unexpected)
                    }
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { _uiState.update { it.copy(isSigningIn = false, failure = AuthFailure.Unexpected) } }
        }
    }
}
