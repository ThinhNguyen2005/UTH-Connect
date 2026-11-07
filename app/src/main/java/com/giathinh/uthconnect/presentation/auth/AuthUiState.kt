package com.giathinh.uthconnect.presentation.auth

import com.giathinh.uthconnect.domain.model.StudentProfile
import com.giathinh.uthconnect.domain.repository.VerificationStatus
import com.giathinh.uthconnect.domain.validation.LoginFieldError

// Mã ngôn ngữ hiển thị trên pill VI/EN ở đầu màn đăng nhập. Đây là nhãn ngắn cho
// người dùng, KHÁC với BCP-47 tag đã lưu (`LANGUAGE_TAG_VIETNAMESE`/`_ENGLISH`);
// AuthViewModel là chỗ dịch qua lại giữa hai dạng.
const val LANGUAGE_CODE_VIETNAMESE = "VI"
const val LANGUAGE_CODE_ENGLISH = "EN"

enum class AuthScreenStep {
    /** Step 1: Identifier-first entry with Google SSO, reduced cognitive load. */
    Identifier,

    /** Step 2: Activated student account enters password (the exact "Hello Again!" screen). */
    Password,

    /** Step 3: First-time student completes profile setup. */
    FirstTimeSetup,

    /** Step 4: Password reset via email OTP. */
    ResetPassword,

    /** Step 5: Email verification waiting screen. */
    VerifyEmail,
}

data class AuthUiState(
    val isRestoring: Boolean = true,
    val student: StudentProfile? = null,
    val identifier: String = "",
    val password: String = "",
    val rememberMe: Boolean = true,
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
    val forgotCooldownSeconds: Int = 0,
    /** Google button only appears when the Supabase backend and a Web client ID are configured. */
    val googleSignInAvailable: Boolean = false,
    val isSigningInWithGoogle: Boolean = false,
    /** Signed in with Google or credentials, but no administrator has confirmed the MSSV: browse-only. */
    val googleVerification: GoogleVerificationNotice? = null,
    val verificationStudentId: String = "",
    val isSubmittingVerification: Boolean = false,
    val verificationStatus: VerificationStatus? = null,
    val verificationNotice: String? = null,
    /** Selected display language ([LANGUAGE_CODE_VIETNAMESE] or [LANGUAGE_CODE_ENGLISH]). */
    val selectedLanguage: String = LANGUAGE_CODE_VIETNAMESE,
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
    val resetOtp: String = "",
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

    /**
     * Người dùng vừa chọn ngôn ngữ khác trên pill VI/EN. Không có thông điệp snackbar:
     * Locale áp ở `MainActivity.attachBaseContext` nên Route phải `recreate()`.
     */
    data object LanguageChanged : AuthEvent
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
    VerificationRequired,
}

/** Non-sensitive display data taken from the unverified profile row. */
data class GoogleVerificationNotice(val email: String, val displayName: String)
