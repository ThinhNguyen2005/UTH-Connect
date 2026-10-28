package com.giathinh.uthconnect.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.giathinh.uthconnect.domain.repository.AuthRepository
import com.giathinh.uthconnect.domain.repository.SessionRepository
import com.giathinh.uthconnect.domain.usecase.LoginResult
import com.giathinh.uthconnect.domain.usecase.LoginUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AuthViewModel(
    private val loginUseCase: LoginUseCase,
    private val authRepository: AuthRepository,
    private val sessionRepository: SessionRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val student = try {
                sessionRepository.getRememberedStudentId()?.let { studentId ->
                    authRepository.getStudentProfile(studentId)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                null // Broken session should never block the login screen.
            }
            _uiState.update {
                it.copy(
                    isRestoring = false,
                    student = student,
                    rememberMe = student != null,
                )
            }
        }
    }

    fun onIdentifierChanged(value: String) {
        _uiState.update { it.copy(identifier = value, identifierError = null, signInError = null) }
    }

    fun onPasswordChanged(value: String) {
        _uiState.update { it.copy(password = value, passwordError = null, signInError = null) }
    }

    fun onRememberMeChanged(value: Boolean) {
        _uiState.update { it.copy(rememberMe = value) }
    }

    fun togglePasswordVisibility() {
        _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    fun setForgotPasswordDialog(visible: Boolean) {
        _uiState.update { it.copy(showForgotPasswordDialog = visible) }
    }

    fun signIn() {
        val snapshot = _uiState.value
        if (snapshot.isRestoring || snapshot.isSigningIn || snapshot.student != null) return
        _uiState.update { it.copy(isSigningIn = true, signInError = null) }

        viewModelScope.launch {
            try {
                when (val result = loginUseCase(snapshot.identifier, snapshot.password)) {
                    is LoginResult.InvalidForm -> _uiState.update {
                        it.copy(
                            isSigningIn = false,
                            identifierError = result.identifierError,
                            passwordError = result.passwordError,
                        )
                    }
                    LoginResult.InvalidCredentials -> _uiState.update {
                        it.copy(isSigningIn = false, signInError = SignInError.INVALID_CREDENTIALS)
                    }
                    is LoginResult.Success -> {
                        sessionRepository.save(result.profile.studentId, snapshot.rememberMe)
                        _uiState.update {
                            it.copy(
                                student = result.profile,
                                isSigningIn = false,
                                identifierError = null,
                                passwordError = null,
                                password = "", // Drop plaintext from state immediately.
                                destination = Destination.HOME,
                            )
                        }
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(isSigningIn = false, signInError = SignInError.UNEXPECTED)
                }
            }
        }
    }

    fun openProfile() {
        _uiState.update { it.copy(destination = Destination.PROFILE) }
    }

    fun backHome() {
        _uiState.update { it.copy(destination = Destination.HOME) }
    }

    fun signOut() {
        viewModelScope.launch {
            try {
                sessionRepository.clear()
                _uiState.value = AuthUiState(isRestoring = false)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                // Do not show login while an old remembered session remains on disk.
                _uiState.update { it.copy(signInError = SignInError.UNEXPECTED) }
            }
        }
    }
}
