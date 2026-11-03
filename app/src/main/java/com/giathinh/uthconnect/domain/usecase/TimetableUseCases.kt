package com.giathinh.uthconnect.domain.usecase

import com.giathinh.uthconnect.domain.model.CourseSchedule
import com.giathinh.uthconnect.domain.model.Deadline
import com.giathinh.uthconnect.domain.repository.TimetableRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class TimetableUseCases @Inject constructor(
    private val repository: TimetableRepository,
) {
    fun observeSchedules(): Flow<List<CourseSchedule>> = repository.observeSchedules()

    fun observeDeadlines(): Flow<List<Deadline>> = repository.observeDeadlines()

    suspend fun saveSchedule(schedule: CourseSchedule): Result<Unit> = repository.saveSchedule(schedule)

    suspend fun deleteSchedule(id: String): Result<Unit> = repository.deleteSchedule(id)

    suspend fun saveDeadline(deadline: Deadline): Result<Unit> = repository.saveDeadline(deadline)

    suspend fun deleteDeadline(id: String): Result<Unit> = repository.deleteDeadline(id)

    suspend fun refreshReminders(): Result<Unit> = repository.refreshReminders()
}
