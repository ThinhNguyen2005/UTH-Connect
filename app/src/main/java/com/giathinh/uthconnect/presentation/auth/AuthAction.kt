package com.giathinh.uthconnect.presentation.auth

/** Mỗi tương tác gửi vào ViewModel; UI không gọi Repository trực tiếp. */
sealed interface AuthAction {
    data class IdentifierChanged(val value: String) : AuthAction
    data class PasswordChanged(val value: String) : AuthAction
    data class RememberChanged(val value: Boolean) : AuthAction
    data object TogglePassword : AuthAction
    data object Submit : AuthAction
    data object ForgotPassword : AuthAction
    data object DismissForgotPassword : AuthAction
}
