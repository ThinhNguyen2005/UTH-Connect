package com.giathinh.uthconnect.presentation.auth.forgotpassword
import com.giathinh.uthconnect.domain.repository.AuthError

data class ForgotPasswordUiState(val email: String = "", val isSending: Boolean = false, val sent: Boolean = false, val error: AuthError? = null)
