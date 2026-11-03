package com.giathinh.uthconnect.data.local.room

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduleDao {
    @Query("SELECT * FROM course_schedules ORDER BY dayOfWeek, startPeriod")
    fun observeAll(): Flow<List<ScheduleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(schedule: ScheduleEntity)

    @Query("DELETE FROM course_schedules WHERE id = :id")
    suspend fun deleteById(id: String)
}
