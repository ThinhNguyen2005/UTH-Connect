package com.giathinh.uthconnect.data.repository

import com.giathinh.uthconnect.data.local.room.TimetableDatabase
import com.giathinh.uthconnect.data.local.room.DeadlineEntity
import com.giathinh.uthconnect.data.local.room.ScheduleEntity
import com.giathinh.uthconnect.data.notification.TimetableReminderScheduler
import com.giathinh.uthconnect.domain.model.CourseSchedule
import com.giathinh.uthconnect.domain.model.Deadline
import com.giathinh.uthconnect.domain.repository.TimetableRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.CancellationException
import javax.inject.Inject

class RoomTimetableRepository @Inject constructor(
    private val database: TimetableDatabase,
    private val reminders: TimetableReminderScheduler,
) : TimetableRepository {
    override fun observeSchedules(): Flow<List<CourseSchedule>> =
        database.scheduleDao().observeAll().map { rows -> rows.map(ScheduleEntity::toDomain) }

    override fun observeDeadlines(): Flow<List<Deadline>> =
        database.deadlineDao().observeAll().map { rows -> rows.map(DeadlineEntity::toDomain) }

    override suspend fun saveSchedule(schedule: CourseSchedule): Result<Unit> = persist {
        database.scheduleDao().upsert(ScheduleEntity.fromDomain(schedule))
    }.onSuccess { reminders.scheduleClass(schedule) }

    override suspend fun deleteSchedule(id: String): Result<Unit> = persist {
        database.scheduleDao().deleteById(id)
    }.onSuccess { reminders.cancelClass(id) }

    override suspend fun saveDeadline(deadline: Deadline): Result<Unit> = persist {
        database.deadlineDao().upsert(DeadlineEntity.fromDomain(deadline))
    }.onSuccess { reminders.scheduleDeadline(deadline) }

    override suspend fun deleteDeadline(id: String): Result<Unit> = persist {
        database.deadlineDao().deleteById(id)
    }.onSuccess { reminders.cancelDeadline(id) }

    override suspend fun refreshReminders(): Result<Unit> = persist {
        database.scheduleDao().observeAll().first().forEach { reminders.scheduleClass(it.toDomain()) }
        database.deadlineDao().observeAll().first().forEach { reminders.scheduleDeadline(it.toDomain()) }
    }

    private suspend fun persist(write: suspend () -> Unit): Result<Unit> = try {
        write()
        Result.success(Unit)
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (failure: Exception) {
        Result.failure(failure)
    }
}
