package com.giathinh.uthconnect.presentation.auth

import com.giathinh.uthconnect.domain.model.StudentProfile
import com.giathinh.uthconnect.domain.validation.LoginFieldError

data class AuthUiState(
    val isRestoring: Boolean = true,
    val isSignUp: Boolean = false,
    val pendingIdentity: com.giathinh.uthconnect.domain.model.AuthIdentity? = null,
    val student: StudentProfile? = null,
    val identifier: String = "",
    val password: String = "",
    val rememberMe: Boolean = false,
    val passwordVisible: Boolean = false,
    val isSigningIn: Boolean = false,
    val identifierError: LoginFieldError? = null,
    val passwordError: LoginFieldError? = null,
    val failure: AuthFailure? = null,
    val forgotDialogOpen: Boolean = false,
    val resetEmail: String = "",
    val isResetting: Boolean = false,
    val resetSent: Boolean = false,
    /** Enrollment is persisted; NOT a signed-in session until OS prompt succeeds. */
    val biometricAvailable: Boolean = false,
)

/** One-off notification (not a navigation command; the signed-in gate is state driven). */
sealed interface AuthEvent {
    data object InvalidCredentials : AuthEvent
    data object RequestGoogle : AuthEvent
    data object ClearCredentials : AuthEvent
}

enum class AuthFailure {
    InvalidCredentials, Unexpected, SessionRestore, BiometricUnavailable, Configuration, Network,
    Disabled, InvalidSession, VerificationRequired, ApprovalRequired, ProfileMissing, PermissionDenied,
    TooManyRequests, StudentIdUnavailable, InvalidResetEmail, DomainNotAllowed, EmailAlreadyInUse, WeakPassword, AccountCollision, Unknown,
}
