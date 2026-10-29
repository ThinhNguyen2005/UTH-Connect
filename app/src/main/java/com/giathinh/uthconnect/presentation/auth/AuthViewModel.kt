package com.giathinh.uthconnect.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.giathinh.uthconnect.domain.repository.SessionRepository
import com.giathinh.uthconnect.domain.usecase.SignInResult
import com.giathinh.uthconnect.domain.usecase.SignInUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** StateFlow một nguồn sự thật. Không biết Context hoặc Composable. */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val signIn: SignInUseCase,
    private val session: SessionRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            session.restore()
            session.session.collect { current ->
                _uiState.update {
                    it.copy(
                        isRestoring = current.isRestoring,
                        student = current.profile,
                        rememberMe = if (current.profile != null) current.remembered else it.rememberMe,
                        failure = if (current.failure != null) AuthFailure.SessionRestore else it.failure,
                    )
                }
            }
        }
    }

    fun onAction(action: AuthAction) {
        when (action) {
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
            AuthAction.Submit -> submit()
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
                    SignInResult.InvalidCredentials -> _uiState.update {
                        it.copy(isSigningIn = false, failure = AuthFailure.InvalidCredentials)
                    }
                    SignInResult.Failure -> _uiState.update {
                        it.copy(isSigningIn = false, failure = AuthFailure.Unexpected)
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _uiState.update { it.copy(isSigningIn = false, failure = AuthFailure.Unexpected) }
            }
        }
    }
}
