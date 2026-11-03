package com.giathinh.uthconnect.data.di

import android.content.Context
import androidx.room.Room
import com.giathinh.uthconnect.data.local.room.TimetableDatabase
import com.giathinh.uthconnect.data.repository.RoomTimetableRepository
import com.giathinh.uthconnect.domain.repository.TimetableRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class TimetableDataModule {
    @Binds
    @Singleton
    abstract fun bindTimetableRepository(implementation: RoomTimetableRepository): TimetableRepository

    companion object {
        @Provides
        @Singleton
        fun provideTimetableDatabase(@ApplicationContext context: Context): TimetableDatabase =
            Room.databaseBuilder(context, TimetableDatabase::class.java, "timetable.db")
                .addMigrations(TimetableDatabase.MIGRATION_1_2)
                .build()
    }
}
