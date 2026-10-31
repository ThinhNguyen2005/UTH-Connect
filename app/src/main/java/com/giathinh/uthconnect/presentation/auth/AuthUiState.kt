package com.giathinh.uthconnect.presentation.auth

import com.giathinh.uthconnect.domain.model.StudentProfile
import com.giathinh.uthconnect.domain.validation.LoginFieldError

/** Chỉ có một UiState cho màn đăng nhập; mặc định không giả vờ đã có phiên. */
data class AuthUiState(
    val isRestoring: Boolean = true,
    val student: StudentProfile? = null,
    val identifier: String = "",
    val password: String = "",
    val rememberMe: Boolean = false,
    val passwordVisible: Boolean = false,
    val isSigningIn: Boolean = false,
    val identifierError: LoginFieldError? = null,
    val passwordError: LoginFieldError? = null,
    val failure: AuthFailure? = null,
    val forgotDialogOpen: Boolean = false,
)

enum class AuthFailure { InvalidCredentials, Unexpected, SessionRestore }
