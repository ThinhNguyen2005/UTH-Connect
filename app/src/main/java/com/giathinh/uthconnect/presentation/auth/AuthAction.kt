package com.giathinh.uthconnect.presentation.auth

sealed interface AuthAction {
    data class IdentifierChanged(val value: String) : AuthAction
    data class PasswordChanged(val value: String) : AuthAction
    data class RememberChanged(val value: Boolean) : AuthAction
    data object TogglePassword : AuthAction
    data object Submit : AuthAction

    /** Repeats a failed sign-in attempt after the user corrects credentials. */
    data object Retry : AuthAction
    data object ForgotPassword : AuthAction
    data object DismissForgotPassword : AuthAction
    data class ForgotEmailChanged(val value: String) : AuthAction
    data object SubmitForgotPassword : AuthAction

    /** Route performs Credential Manager; the VM only receives its outcome. */
    data object RequestGoogleSignIn : AuthAction
    data class GoogleIdTokenReceived(val idToken: String, val rawNonce: String) : AuthAction
    data object GoogleSignInCancelled : AuthAction
    data object GoogleSignInUnavailable : AuthAction

    /** Switch active interface language ("VI" or "EN"). */
    data class SelectLanguage(val lang: String) : AuthAction

    /** Clears active account credentials and error state. */
    data object SwitchAccount : AuthAction

    /** Next-step flow actions for cognitive load reduction & first-time setup */
    data object ContinueIdentifier : AuthAction
    data class SelectStep(val step: AuthScreenStep) : AuthAction
    data class NewStudentNameChanged(val value: String) : AuthAction
    data class NewStudentFacultyChanged(val value: String) : AuthAction
    data class ConfirmPasswordChanged(val value: String) : AuthAction
    data object SubmitFirstTimeSetup : AuthAction
    data object ClearSetupNotice : AuthAction

    /** Verification request actions */
    data class VerificationStudentIdChanged(val value: String) : AuthAction
    data object SubmitVerificationRequest : AuthAction

    /** Reset password actions */
    data class ResetNewPasswordChanged(val value: String) : AuthAction
    data class ResetConfirmPasswordChanged(val value: String) : AuthAction
    data object ToggleResetPasswordVisibility : AuthAction
    data object SubmitResetPassword : AuthAction

    /** Email confirmation actions */
    data object ResendVerificationEmail : AuthAction
    data class DeepLinkReceived(val token: String, val type: String) : AuthAction
}
