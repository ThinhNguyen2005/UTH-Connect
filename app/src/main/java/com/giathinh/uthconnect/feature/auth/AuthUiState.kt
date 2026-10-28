package com.giathinh.uthconnect.feature.auth

import com.giathinh.uthconnect.domain.model.StudentProfile
import com.giathinh.uthconnect.domain.validation.InputError

enum class Destination { HOME, PROFILE }
enum class SignInError { INVALID_CREDENTIALS, UNEXPECTED }

/** Single source of truth for the three screens (UDF). */
data class AuthUiState(
    val isRestoring: Boolean = true,
    val identifier: String = "",
    val password: String = "",
    val rememberMe: Boolean = false,
    val isPasswordVisible: Boolean = false,
    val identifierError: InputError? = null,
    val passwordError: InputError? = null,
    val signInError: SignInError? = null,
    val isSigningIn: Boolean = false,
    val student: StudentProfile? = null,
    val destination: Destination = Destination.HOME,
    val showForgotPasswordDialog: Boolean = false,
)
