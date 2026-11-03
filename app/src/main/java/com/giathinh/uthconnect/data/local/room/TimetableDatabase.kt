package com.giathinh.uthconnect.data.local.room

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [ScheduleEntity::class, DeadlineEntity::class], version = 1, exportSchema = false)
abstract class TimetableDatabase : RoomDatabase() {
    abstract fun scheduleDao(): ScheduleDao
    abstract fun deadlineDao(): DeadlineDao
}
