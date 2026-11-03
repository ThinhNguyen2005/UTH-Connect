package com.giathinh.uthconnect.data.local.room

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [ScheduleEntity::class, DeadlineEntity::class], version = 2, exportSchema = false)
abstract class TimetableDatabase : RoomDatabase() {
    abstract fun scheduleDao(): ScheduleDao
    abstract fun deadlineDao(): DeadlineDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE deadlines ADD COLUMN dueTime TEXT NOT NULL DEFAULT '23:59'")
            }
        }
    }
}
