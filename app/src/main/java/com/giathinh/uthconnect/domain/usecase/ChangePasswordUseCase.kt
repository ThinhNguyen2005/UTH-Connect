package com.giathinh.uthconnect.domain.usecase

import com.giathinh.uthconnect.domain.repository.AuthRepository
import com.giathinh.uthconnect.domain.repository.SessionRepository
import com.giathinh.uthconnect.domain.validation.PasswordChangeValidator
import com.giathinh.uthconnect.domain.validation.PasswordValidation
import javax.inject.Inject

sealed interface ChangePasswordResult {
    data class InvalidForm(val validation: PasswordValidation) : ChangePasswordResult
    data object IncorrectCurrentPassword : ChangePasswordResult
    data object NoSession : ChangePasswordResult
    data object Success : ChangePasswordResult
    data object Failed : ChangePasswordResult
}

class ChangePasswordUseCase @Inject constructor(
    private val auth: AuthRepository,
    private val sessions: SessionRepository,
) {
    private val validator = PasswordChangeValidator()
    suspend operator fun invoke(old: String, updated: String, confirmed: String): ChangePasswordResult {
        val validation = validator.validate(old, updated, confirmed)
        if (!validation.valid) return ChangePasswordResult.InvalidForm(validation)
        val id = sessions.session.value.profile?.studentId ?: return ChangePasswordResult.NoSession
        return auth.changePassword(id, old, updated).fold(
            onSuccess = { if (it) ChangePasswordResult.Success else ChangePasswordResult.IncorrectCurrentPassword },
            onFailure = { ChangePasswordResult.Failed },
        )
    }
}
