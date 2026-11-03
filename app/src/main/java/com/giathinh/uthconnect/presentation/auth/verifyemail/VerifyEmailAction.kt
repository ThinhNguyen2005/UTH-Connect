package com.giathinh.uthconnect.presentation.auth.verifyemail

sealed interface VerifyEmailAction {
    data object Resend : VerifyEmailAction
    data class Check(val remember: Boolean) : VerifyEmailAction
    data object SignOut : VerifyEmailAction
}
