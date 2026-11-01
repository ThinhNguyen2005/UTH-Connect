package com.giathinh.uthconnect.presentation.profile

sealed interface ProfileAction {
    data object OpenPassword : ProfileAction
    data object OpenContact : ProfileAction
    data object Back : ProfileAction
    data class OldPasswordChanged(val value: String) : ProfileAction
    data class NewPasswordChanged(val value: String) : ProfileAction
    data class ConfirmPasswordChanged(val value: String) : ProfileAction
    data object SubmitPassword : ProfileAction
    data class PhoneChanged(val value: String) : ProfileAction
    data class EmailChanged(val value: String) : ProfileAction
    data class AddressChanged(val value: String) : ProfileAction
    data object SubmitContact : ProfileAction
    data object RequestBiometricEnroll : ProfileAction
    data object BiometricVerified : ProfileAction
    data object BiometricFailed : ProfileAction
    data object DisableBiometric : ProfileAction
    data object RequestLogout : ProfileAction
    data object CancelLogout : ProfileAction
    data object ConfirmLogout : ProfileAction
    data object RetryLogout : ProfileAction
    data object Logout : ProfileAction // legacy alias: now requests confirmation, not immediate logout
}
