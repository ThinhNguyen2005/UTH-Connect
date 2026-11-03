package com.giathinh.uthconnect.presentation.auth.forgotpassword
sealed interface ForgotPasswordAction {
    data class EmailChanged(val value: String) : ForgotPasswordAction
    data object Send : ForgotPasswordAction
    data object Clear : ForgotPasswordAction
}
