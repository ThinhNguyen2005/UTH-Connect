package com.giathinh.uthconnect.domain.usecase

import com.giathinh.uthconnect.domain.model.IncidentDraft
import com.giathinh.uthconnect.domain.model.IncidentReport
import com.giathinh.uthconnect.domain.repository.IncidentRepository
import com.giathinh.uthconnect.domain.validation.IncidentReportValidator
import com.giathinh.uthconnect.domain.validation.IncidentValidation
import javax.inject.Inject

class InvalidIncidentReportException(
    val validation: IncidentValidation,
) : IllegalArgumentException("Invalid incident report")

class CreateIncidentReportUseCase @Inject constructor(
    private val repository: IncidentRepository,
) {
    suspend operator fun invoke(draft: IncidentDraft): Result<IncidentReport> {
        val validation = IncidentReportValidator().validate(
            campus = draft.campus,
            clientRequestId = draft.clientRequestId,
            roomCode = draft.roomCode,
            title = draft.title,
            description = draft.description,
            imageCount = draft.imagePaths.size,
        )
        if (!validation.valid) return Result.failure(InvalidIncidentReportException(validation))
        return repository.createReport(
            draft.copy(
                campus = draft.campus.trim(),
                roomCode = validation.normalizedRoomCode,
                title = draft.title.trim(),
                description = draft.description.trim(),
            ),
        )
    }
}
