package com.giathinh.uthconnect.presentation.auth.verifyemail

import com.giathinh.uthconnect.domain.repository.AuthError

data class VerifyEmailUiState(
    val email: String = "", val isChecking: Boolean = false, val isSending: Boolean = false,
    val cooldownSeconds: Int = 0, val error: AuthError? = null, val emailSent: Boolean = false,
)
