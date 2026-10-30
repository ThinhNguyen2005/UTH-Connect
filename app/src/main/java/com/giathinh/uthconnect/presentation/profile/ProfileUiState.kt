package com.giathinh.uthconnect.presentation.profile

import com.giathinh.uthconnect.domain.model.StudentProfile

/** Mặc định loading khác rõ empty. */
data class ProfileUiState(
    val isLoading: Boolean = true,
    val student: StudentProfile? = null,
    val isLoggingOut: Boolean = false,
    val failure: ProfileFailure? = null,
) {
    val isEmpty get() = !isLoading && student == null && failure == null
}

enum class ProfileFailure { LogoutFailed, LoadFailed }
