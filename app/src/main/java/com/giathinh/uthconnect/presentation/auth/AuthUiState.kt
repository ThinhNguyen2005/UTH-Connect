package com.giathinh.uthconnect.presentation.auth

import com.giathinh.uthconnect.domain.model.StudentProfile
import com.giathinh.uthconnect.domain.validation.LoginFieldError

enum class AuthMode {
    Password,
    Biometric,
}

data class AuthUiState(
    val isRestoring: Boolean = true,
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
    /** Enrollment is persisted; NOT a signed-in session until OS prompt succeeds. */
    val biometricAvailable: Boolean = false,
    /** Google button only appears when the Supabase backend and a Web client ID are configured. */
    val googleSignInAvailable: Boolean = false,
    val isSigningInWithGoogle: Boolean = false,
    /** Signed in with Google, but no administrator has confirmed the MSSV: browse-only. */
    val googleVerification: GoogleVerificationNotice? = null,
    /** Current authentication mode: Password or Biometric. */
    val authMode: AuthMode = AuthMode.Password,
    /** Saved student profile shown in Biometric mode. */
    val savedStudent: StudentProfile? = null,
    /** Selected display language ("VI" or "EN"). */
    val selectedLanguage: String = "VI",
)

/** One-off notification (not a navigation command; the signed-in gate is state driven). */
sealed interface AuthEvent { data object InvalidCredentials : AuthEvent }

enum class AuthFailure {
    InvalidCredentials,
    Unexpected,
    SessionRestore,
    BiometricUnavailable,
    /** Credential Manager has no Google account on this device. */
    GoogleUnavailable,
    /** Supabase rejected the ID token, or the exchange failed. */
    GoogleRejected,
}

/** Non-sensitive display data taken from the unverified profile row. */
data class GoogleVerificationNotice(val email: String, val displayName: String)
