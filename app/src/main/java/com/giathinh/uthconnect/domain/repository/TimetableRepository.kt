package com.giathinh.uthconnect.domain.repository

import com.giathinh.uthconnect.domain.model.CourseSchedule
import com.giathinh.uthconnect.domain.model.Deadline
import kotlinx.coroutines.flow.Flow

interface TimetableRepository {
    fun observeSchedules(): Flow<List<CourseSchedule>>
    fun observeDeadlines(): Flow<List<Deadline>>
    suspend fun saveSchedule(schedule: CourseSchedule): Result<Unit>
    suspend fun deleteSchedule(id: String): Result<Unit>
    suspend fun saveDeadline(deadline: Deadline): Result<Unit>
    suspend fun deleteDeadline(id: String): Result<Unit>
}
