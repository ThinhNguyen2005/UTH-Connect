package com.giathinh.uthconnect.presentation.profile

import com.giathinh.uthconnect.domain.model.StudentProfile
import com.giathinh.uthconnect.domain.validation.ContactValidation
import com.giathinh.uthconnect.domain.validation.PasswordValidation

enum class ProfilePage { Overview, ChangePassword, EditContact }
enum class ProfileFailure { LogoutFailed, LoadFailed, SaveFailed, BiometricFailed }
/** Each event is consumed once by ProfileRoute. Keep persistent form failures in UiState. */
sealed interface ProfileEvent {
    data object LogoutFailed : ProfileEvent
    data object PasswordChanged : ProfileEvent
    data object ContactSaved : ProfileEvent
}

enum class ProfileNotice { PasswordChanged, ContactSaved, BiometricEnabled, BiometricDisabled }

data class PasswordForm(
    val current: String = "",
    val updated: String = "",
    val confirmed: String = "",
    val errors: PasswordValidation = PasswordValidation(),
    val incorrectCurrent: Boolean = false,
)

data class ContactForm(
    val phone: String = "",
    val email: String = "",
    val address: String = "",
    val errors: ContactValidation = ContactValidation(),
)

data class ProfileUiState(
    val isLoading: Boolean = true,
    val student: StudentProfile? = null,
    val isLoggingOut: Boolean = false,
    val failure: ProfileFailure? = null,
    val page: ProfilePage = ProfilePage.Overview,
    val isSaving: Boolean = false,
    val passwordForm: PasswordForm = PasswordForm(),
    val contactForm: ContactForm = ContactForm(),
    val confirmLogout: Boolean = false,
    val biometricEnabled: Boolean = false,
    val notice: ProfileNotice? = null,
) {
    val isEmpty get() = !isLoading && student == null && failure == null
}
