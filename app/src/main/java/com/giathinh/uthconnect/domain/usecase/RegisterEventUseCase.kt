package com.giathinh.uthconnect.domain.usecase

import com.giathinh.uthconnect.domain.model.Registration
import com.giathinh.uthconnect.domain.repository.StudentIdProvider
import java.time.LocalDateTime
import java.util.UUID
import javax.inject.Inject

/** Creates a local registration stub. Its event and student IDs can be used by Check-in later. */
class RegisterEventUseCase @Inject constructor(
    private val getEvents: GetEventsUseCase,
    private val studentIdProvider: StudentIdProvider,
) {
    suspend operator fun invoke(eventId: String): Result<Registration> {
        val studentId = studentIdProvider.studentId().getOrElse { return Result.failure(it) }.trim()
        if (studentId.isBlank()) {
            return Result.failure(IllegalStateException("Student ID is missing"))
        }

        val event = getEvents().getOrElse { return Result.failure(it) }
            .firstOrNull { it.id == eventId }
            ?: return Result.failure(IllegalArgumentException("Event was not found"))

        if (!event.canRegister) {
            return Result.failure(IllegalStateException("Registration is closed"))
        }

        return Result.success(
            Registration(
                id = UUID.randomUUID().toString(),
                eventId = event.id,
                studentId = studentId,
                registeredAt = LocalDateTime.now(),
            ),
        )
    }
}
