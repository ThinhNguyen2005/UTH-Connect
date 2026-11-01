package com.giathinh.uthconnect.data.di

import com.giathinh.uthconnect.data.repository.InMemoryTimetableRepository
import com.giathinh.uthconnect.domain.repository.TimetableRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import java.time.ZoneId
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object TimetableDataModule {
    @Provides @Singleton @Named("timetable")
    fun clock(): Clock = Clock.system(ZoneId.of("Asia/Ho_Chi_Minh"))
    @Provides @Singleton
    fun repository(@Named("timetable") clock: Clock): TimetableRepository = InMemoryTimetableRepository(clock)
}
