package com.giathinh.uthconnect.presentation.auth

sealed interface AuthAction {
    data class ChangeMode(val signUp: Boolean) : AuthAction
    data object RequestGoogle : AuthAction
    data class GoogleTokenReceived(val token: String) : AuthAction
    data class GoogleFailed(val configuration: Boolean = false) : AuthAction
    data object GoogleCancelled : AuthAction
    data class IdentifierChanged(val value: String) : AuthAction
    data class PasswordChanged(val value: String) : AuthAction
    data class RememberChanged(val value: Boolean) : AuthAction
    data object TogglePassword : AuthAction
    data object Submit : AuthAction
    data object RetryRestore : AuthAction
    data object RefreshSession : AuthAction
    data class ResetEmailChanged(val value: String) : AuthAction
    data object SubmitReset : AuthAction
    /** Repeats a failed sign-in attempt after the user corrects credentials. */
    data object Retry : AuthAction
    data object ForgotPassword : AuthAction
    data object DismissForgotPassword : AuthAction
    /** Route performs Android BiometricPrompt. VM never invokes OS APIs. */
    data object RequestBiometric : AuthAction
    data object BiometricVerified : AuthAction
    data object BiometricFailed : AuthAction
}
