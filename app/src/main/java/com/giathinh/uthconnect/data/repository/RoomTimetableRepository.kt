package com.giathinh.uthconnect.data.repository

import com.giathinh.uthconnect.data.local.room.TimetableDatabase
import com.giathinh.uthconnect.data.local.room.DeadlineEntity
import com.giathinh.uthconnect.data.local.room.ScheduleEntity
import com.giathinh.uthconnect.domain.model.CourseSchedule
import com.giathinh.uthconnect.domain.model.Deadline
import com.giathinh.uthconnect.domain.repository.TimetableRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class RoomTimetableRepository @Inject constructor(
    private val database: TimetableDatabase,
) : TimetableRepository {
    override fun observeSchedules(): Flow<List<CourseSchedule>> =
        database.scheduleDao().observeAll().map { rows -> rows.map(ScheduleEntity::toDomain) }

    override fun observeDeadlines(): Flow<List<Deadline>> =
        database.deadlineDao().observeAll().map { rows -> rows.map(DeadlineEntity::toDomain) }

    override suspend fun saveSchedule(schedule: CourseSchedule): Result<Unit> = runCatching {
        database.scheduleDao().upsert(ScheduleEntity.fromDomain(schedule))
    }

    override suspend fun deleteSchedule(id: String): Result<Unit> = runCatching {
        database.scheduleDao().deleteById(id)
    }

    override suspend fun saveDeadline(deadline: Deadline): Result<Unit> = runCatching {
        database.deadlineDao().upsert(DeadlineEntity.fromDomain(deadline))
    }

    override suspend fun deleteDeadline(id: String): Result<Unit> = runCatching {
        database.deadlineDao().deleteById(id)
    }
}
