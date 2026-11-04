package com.giathinh.uthconnect.domain.repository

import com.giathinh.uthconnect.domain.model.AttendanceRoster

interface AttendanceRosterRepository {
    suspend fun getRoster(eventId: String): Result<AttendanceRoster>
}
