package com.giathinh.uthconnect.domain.usecase

import com.giathinh.uthconnect.domain.repository.AttendanceRosterRepository
import javax.inject.Inject

class GetAttendanceRosterUseCase @Inject constructor(private val repository: AttendanceRosterRepository) {
    suspend operator fun invoke(eventId: String) = repository.getRoster(eventId)
}
