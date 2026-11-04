package com.giathinh.uthconnect.presentation.auth

import com.giathinh.uthconnect.domain.model.StudentProfile
import com.giathinh.uthconnect.domain.repository.VerificationStatus
import com.giathinh.uthconnect.domain.validation.LoginFieldError


enum class AuthScreenStep {
    /** Step 1: Identifier-first entry with Google SSO, reduced cognitive load. */
    Identifier,
    /** Step 2: Activated student account enters password (the exact "Hello Again!" screen). */
    Password,
    /** Step 3: First-time student completes profile setup. */
    FirstTimeSetup,
    /** Step 4: Password reset via deep link. */
    ResetPassword,
    /** Step 5: Email verification waiting screen. */
    VerifyEmail,
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
    val forgotEmail: String = "",
    val isSendingForgot: Boolean = false,
    val forgotSuccessNotice: Boolean = false,
    val forgotError: String? = null,
    /** Google button only appears when the Supabase backend and a Web client ID are configured. */
    val googleSignInAvailable: Boolean = false,
    val isSigningInWithGoogle: Boolean = false,
    /** Signed in with Google or credentials, but no administrator has confirmed the MSSV: browse-only. */
    val googleVerification: GoogleVerificationNotice? = null,
    val verificationStudentId: String = "",
    val isSubmittingVerification: Boolean = false,
    val verificationStatus: VerificationStatus? = null,
    val verificationNotice: String? = null,
    /** Selected display language ("VI" or "EN"). */
    val selectedLanguage: String = "VI",
    /** Active step in the identifier-first auth flow. */
    val screenStep: AuthScreenStep = AuthScreenStep.Password,
    /** First-time setup student name. */
    val newStudentName: String = "",
    /** First-time setup faculty/major. */
    val newStudentFaculty: String = "",
    /** First-time setup confirm password. */
    val confirmPassword: String = "",
    /** First-time setup success banner flag. */
    val setupSuccessNotice: Boolean = false,
    val registeredEmail: String = "",
    val verifyEmailCooldownSeconds: Int = 0,
    /** Reset password fields */
    val resetNewPassword: String = "",
    val resetConfirmPassword: String = "",
    val resetPasswordVisible: Boolean = false,
    val isResettingPassword: Boolean = false,
    val resetPasswordError: String? = null,
    val resetSuccessNotice: Boolean = false,
)

/** One-off notification (not a navigation command; the signed-in gate is state driven). */
sealed interface AuthEvent {
    data object InvalidCredentials : AuthEvent
    data class GeneralMessage(val message: String) : AuthEvent
}

enum class AuthFailure {
    InvalidCredentials,
    Unexpected,
    SessionRestore,
    /** Credential Manager has no Google account on this device. */
    GoogleUnavailable,
    /** Supabase rejected the ID token, or the exchange failed. */
    GoogleRejected,
    RateLimited,
    NetworkError,
    TokenExpired,
}

/** Non-sensitive display data taken from the unverified profile row. */
data class GoogleVerificationNotice(val email: String, val displayName: String)
