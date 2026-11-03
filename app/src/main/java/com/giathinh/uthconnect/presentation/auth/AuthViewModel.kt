package com.giathinh.uthconnect.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.giathinh.uthconnect.domain.usecase.AccessStudentSessionUseCase
import com.giathinh.uthconnect.domain.usecase.SignInResult
import com.giathinh.uthconnect.domain.repository.AuthError
import com.giathinh.uthconnect.domain.repository.AuthException
import com.giathinh.uthconnect.domain.usecase.ResetPasswordUseCase
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
    private val resetPassword: ResetPasswordUseCase,
    private val signUp: com.giathinh.uthconnect.domain.usecase.SignUpUseCase? = null,
    private val google: com.giathinh.uthconnect.domain.usecase.SignInWithGoogleUseCase? = null,
    private val account: com.giathinh.uthconnect.domain.usecase.ManageAuthAccountUseCase? = null,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()
    private val eventChannel = Channel<AuthEvent>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()

    private var googleJob: kotlinx.coroutines.Job? = null
    private var refreshing = false
    init {
        account?.let { manager -> viewModelScope.launch {
            manager.observePendingIdentity().collect { identity ->
                val previous = _uiState.value.pendingIdentity
                _uiState.update { it.copy(pendingIdentity = identity,
                    password = if (identity != null) "" else it.password,
                    passwordVisible = if (identity != null) false else it.passwordVisible) }
                if (previous != null && identity == null && _uiState.value.student == null) {
                    eventChannel.send(AuthEvent.ClearCredentials)
                }
            }
        } }
        viewModelScope.launch { sessions.monitorAuthChanges() }
        viewModelScope.launch {
            try { sessions.restore() }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { _uiState.update { it.copy(isRestoring = false, failure = AuthFailure.SessionRestore) } }
            sessions.observe().collect { current ->
                val signedOut = _uiState.value.student != null && current.profile == null
                if (signedOut) eventChannel.send(AuthEvent.ClearCredentials)
                _uiState.update {
                    val justLoggedOut = it.student != null && current.profile == null
                    it.copy(
                        isRestoring = current.isRestoring,
                        student = current.profile,
                        identifier = if (justLoggedOut) "" else it.identifier,
                        password = if (justLoggedOut) "" else it.password,
                        passwordVisible = if (justLoggedOut) false else it.passwordVisible,
                        biometricAvailable = current.biometricStudentId != null,
                        rememberMe = current.remembered,
                        failure = if (current.failure != null) AuthFailure.SessionRestore
                            else if (it.failure == AuthFailure.SessionRestore || current.profile != null) null else it.failure,
                    )
                }
            }
        }
    }

    fun onAction(action: AuthAction) {
        when (action) {
            is AuthAction.ChangeMode -> if (!_uiState.value.isSigningIn) _uiState.update {
                it.copy(isSignUp = action.signUp, failure = null, identifierError = null, passwordError = null)
            }
            AuthAction.RequestGoogle -> requestGoogle()
            is AuthAction.GoogleTokenReceived -> googleSignIn(action.token)
            AuthAction.GoogleCancelled -> _uiState.update { it.copy(isSigningIn = false) }
            is AuthAction.GoogleFailed -> _uiState.update {
                it.copy(isSigningIn = false, failure = if (action.configuration) AuthFailure.Configuration else AuthFailure.Unknown)
            }
            is AuthAction.IdentifierChanged -> _uiState.update {
                it.copy(identifier = action.value, identifierError = null, failure = null)
            }
            is AuthAction.PasswordChanged -> _uiState.update {
                it.copy(password = action.value, passwordError = null, failure = null)
            }
            is AuthAction.RememberChanged -> _uiState.update { it.copy(rememberMe = action.value) }
            AuthAction.TogglePassword -> _uiState.update { it.copy(passwordVisible = !it.passwordVisible) }
            AuthAction.ForgotPassword -> _uiState.update { it.copy(forgotDialogOpen = true, resetEmail = it.identifier.takeIf { value -> value.contains('@') }.orEmpty(), resetSent = false, failure = null) }
            AuthAction.DismissForgotPassword -> _uiState.update { it.copy(forgotDialogOpen = false) }
            AuthAction.Submit, AuthAction.Retry -> submit()
            AuthAction.RetryRestore -> restoreAgain()
            AuthAction.RefreshSession -> refreshSession()
            is AuthAction.ResetEmailChanged -> _uiState.update { it.copy(resetEmail = action.value, failure = null, resetSent = false) }
            AuthAction.SubmitReset -> reset()
            AuthAction.RequestBiometric -> Unit // platform prompt belongs to AuthRoute
            AuthAction.BiometricVerified -> biometricUnlock()
            AuthAction.BiometricFailed -> _uiState.update {
                it.copy(isSigningIn = false, failure = AuthFailure.BiometricUnavailable)
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
                if (snapshot.isSignUp) {
                    val validator = com.giathinh.uthconnect.domain.validation.LoginValidator()
                    val emailError = validator.emailError(snapshot.identifier)
                    val passwordError = validator.signUpPasswordError(snapshot.password)
                    if (emailError != null || passwordError != null) {
                        _uiState.update { it.copy(identifierError = emailError, passwordError = passwordError) }
                        return@launch
                    }
                    val result = signUp?.invoke(snapshot.identifier, snapshot.password)
                    result?.fold(onSuccess = { _uiState.update { it.copy(password = "", passwordVisible = false) } },
                        onFailure = { error -> _uiState.update { it.copy(failure = mapFailure((error as? AuthException)?.reason ?: AuthError.Unknown)) } })
                        ?: _uiState.update { it.copy(failure = AuthFailure.Configuration) }
                    return@launch
                }
                when (val outcome = signIn(snapshot.identifier, snapshot.password, snapshot.rememberMe)) {
                    is SignInResult.Success -> _uiState.update {
                        it.copy(isSigningIn = false, password = "", passwordVisible = false, identifierError = null, passwordError = null)
                    }
                    is SignInResult.InvalidForm -> _uiState.update {
                        it.copy(isSigningIn = false, identifierError = outcome.identifier, passwordError = outcome.password)
                    }
                    SignInResult.InvalidCredentials -> {
                        _uiState.update { it.copy(isSigningIn = false, failure = AuthFailure.InvalidCredentials) }
                        eventChannel.send(AuthEvent.InvalidCredentials)
                    }
                    is SignInResult.ServiceError -> _uiState.update {
                        it.copy(isSigningIn = false, failure = mapFailure(outcome.reason))
                    }
                    SignInResult.Failure -> _uiState.update {
                        it.copy(isSigningIn = false, failure = AuthFailure.Unexpected)
                    }
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { _uiState.update { it.copy(failure = AuthFailure.Unexpected) } }
            finally { _uiState.update { it.copy(isSigningIn = false) } }
        }
    }
    private fun restoreAgain() {
        if (_uiState.value.isRestoring || _uiState.value.isSigningIn) return
        _uiState.update { it.copy(isRestoring = true, failure = null) }
        viewModelScope.launch {
            try { sessions.restore() }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { _uiState.update { it.copy(failure = AuthFailure.SessionRestore) } }
            finally { _uiState.update { it.copy(isRestoring = false) } }
        }
    }

    private fun refreshSession() {
        if (refreshing || _uiState.value.isRestoring || _uiState.value.isSigningIn) return
        refreshing = true
        viewModelScope.launch {
            try { sessions.refresh() }
            finally { refreshing = false }
        }
    }

    private fun reset() {
        val snapshot = _uiState.value
        if (snapshot.isResetting || snapshot.isSigningIn) return
        _uiState.update { it.copy(isResetting = true, resetSent = false, failure = null) }
        viewModelScope.launch {
            try {
                resetPassword(snapshot.resetEmail).fold(
                    onSuccess = { _uiState.update { it.copy(resetSent = true) } },
                    onFailure = { error -> _uiState.update { it.copy(failure =
                        (error as? AuthException)?.let { mapped -> mapFailure(mapped.reason) } ?: AuthFailure.Unexpected) } },
                )
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: IllegalArgumentException) { _uiState.update { it.copy(failure = AuthFailure.InvalidResetEmail) } }
            finally { _uiState.update { it.copy(isResetting = false) } }
        }
    }

    private fun mapFailure(reason: AuthError): AuthFailure = when (reason) {
        AuthError.Configuration -> AuthFailure.Configuration
        AuthError.Network -> AuthFailure.Network
        AuthError.Disabled -> AuthFailure.Disabled
        AuthError.InvalidCredentials -> AuthFailure.InvalidCredentials
        AuthError.InvalidSession -> AuthFailure.InvalidSession
        AuthError.VerificationRequired -> AuthFailure.VerificationRequired
        AuthError.ApprovalRequired -> AuthFailure.ApprovalRequired
        AuthError.ProfileMissing -> AuthFailure.ProfileMissing
        AuthError.PermissionDenied -> AuthFailure.PermissionDenied
        AuthError.TooManyRequests -> AuthFailure.TooManyRequests
        AuthError.StudentIdUnavailable -> AuthFailure.StudentIdUnavailable
        AuthError.Unexpected -> AuthFailure.Unexpected
        AuthError.EmailNotVerified -> AuthFailure.VerificationRequired
        AuthError.DomainNotAllowed -> AuthFailure.DomainNotAllowed
        AuthError.EmailAlreadyInUse -> AuthFailure.EmailAlreadyInUse
        AuthError.WeakPassword -> AuthFailure.WeakPassword
        AuthError.AccountCollision -> AuthFailure.AccountCollision
        AuthError.Unknown -> AuthFailure.Unknown
    }

    private fun requestGoogle() {
        if (_uiState.value.isSigningIn || _uiState.value.isRestoring) return
        _uiState.update { it.copy(isSigningIn = true, failure = null) }
        viewModelScope.launch { eventChannel.send(AuthEvent.RequestGoogle) }
    }

    private fun googleSignIn(token: String) {
        if (googleJob?.isActive == true) return
        _uiState.update { it.copy(isSigningIn = true) }
        googleJob = viewModelScope.launch {
            try {
                val result = google?.invoke(token, _uiState.value.rememberMe)
                result?.onFailure { error -> _uiState.update { it.copy(failure =
                    mapFailure((error as? AuthException)?.reason ?: AuthError.Unknown)) } }
                    ?: _uiState.update { it.copy(failure = AuthFailure.Configuration) }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { _uiState.update { it.copy(failure = AuthFailure.Unknown) } }
            finally { _uiState.update { it.copy(isSigningIn = false, password = "") } }
        }
    }

}
