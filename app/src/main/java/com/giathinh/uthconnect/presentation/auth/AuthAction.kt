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
    /** Route performs Android BiometricPrompt. VM never invokes OS APIs. */
    data object RequestBiometric : AuthAction
    data object BiometricVerified : AuthAction
    data object BiometricFailed : AuthAction

    /** Route performs Credential Manager; the VM only receives its outcome. */
    data object RequestGoogleSignIn : AuthAction
    data class GoogleIdTokenReceived(val idToken: String, val rawNonce: String) : AuthAction
    data object GoogleSignInCancelled : AuthAction
    data object GoogleSignInUnavailable : AuthAction

    /** Switch active login mode (Password vs Biometric). */
    data class SelectMode(val mode: AuthMode) : AuthAction
    /** Switch active interface language ("VI" or "EN"). */
    data class SelectLanguage(val lang: String) : AuthAction
    /** Switch to password mode and clear active account. */
    data object SwitchAccount : AuthAction
}
