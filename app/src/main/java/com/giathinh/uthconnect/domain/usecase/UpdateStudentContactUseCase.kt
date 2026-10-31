package com.giathinh.uthconnect.domain.usecase

import com.giathinh.uthconnect.domain.repository.StudentContactRepository
import com.giathinh.uthconnect.domain.repository.SessionRepository
import com.giathinh.uthconnect.domain.validation.ContactValidation
import com.giathinh.uthconnect.domain.validation.StudentContactValidator
import javax.inject.Inject

sealed interface UpdateContactResult {
    data class InvalidForm(val validation: ContactValidation) : UpdateContactResult
    data object NoSession : UpdateContactResult
    data object Success : UpdateContactResult
    data object Failed : UpdateContactResult
}

class UpdateStudentContactUseCase @Inject constructor(
    private val contacts: StudentContactRepository,
    private val session: SessionRepository,
) {
    private val validator = StudentContactValidator()
    suspend operator fun invoke(phone: String, email: String, address: String): UpdateContactResult {
        val validation = validator.validate(phone, email, address)
        if (!validation.valid) return UpdateContactResult.InvalidForm(validation)
        val id = session.session.value.profile?.studentId ?: return UpdateContactResult.NoSession
        return contacts.updateContact(id, phone.trim(), email.trim(), address.trim()).fold(
            onSuccess = { profile ->
                session.updateProfile(profile)
                UpdateContactResult.Success
            },
            onFailure = { UpdateContactResult.Failed },
        )
    }
}
