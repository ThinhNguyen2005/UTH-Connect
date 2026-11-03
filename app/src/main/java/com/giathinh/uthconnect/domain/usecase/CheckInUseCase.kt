package com.giathinh.uthconnect.domain.usecase

import com.giathinh.uthconnect.domain.model.TicketQrPayload
import com.giathinh.uthconnect.domain.model.UserRole
import com.giathinh.uthconnect.domain.repository.SessionRepository
import com.giathinh.uthconnect.domain.repository.TicketCheckInOutcome
import com.giathinh.uthconnect.domain.repository.TicketRepository
import javax.inject.Inject

enum class CheckInResult { SUCCESS, ALREADY_CHECKED_IN, NOT_REGISTERED, INVALID_CODE, FORBIDDEN, ERROR }

class CheckInUseCase @Inject constructor(
    private val session: SessionRepository,
    private val tickets: TicketRepository,
) {
    suspend operator fun invoke(rawCode: String): CheckInResult {
        val profile = session.session.value.profile
        if (profile?.role != UserRole.ORGANIZER) return CheckInResult.FORBIDDEN
        val code = rawCode.trim()
        if (code.isEmpty() || code.length > MAX_CODE_LENGTH) return CheckInResult.INVALID_CODE
        val identifier = if (code.startsWith("UTHCONNECT:")) {
            TicketQrPayload.ticketIdOrNull(code) ?: return CheckInResult.INVALID_CODE
        } else {
            code
        }
        return tickets.checkIn(identifier, profile.studentId).fold(
            onSuccess = { outcome ->
                when (outcome) {
                    TicketCheckInOutcome.SUCCESS -> CheckInResult.SUCCESS
                    TicketCheckInOutcome.ALREADY_CHECKED_IN -> CheckInResult.ALREADY_CHECKED_IN
                    TicketCheckInOutcome.NOT_REGISTERED -> CheckInResult.NOT_REGISTERED
                }
            },
            onFailure = { CheckInResult.ERROR },
        )
    }

    private companion object {
        const val MAX_CODE_LENGTH = 1_024
    }
}
