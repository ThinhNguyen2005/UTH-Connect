package com.giathinh.uthconnect.data.di

import com.giathinh.uthconnect.data.repository.FakeScheduleRepository
import com.giathinh.uthconnect.domain.repository.ScheduleRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ScheduleDataModule {

    @Binds
    @Singleton
    abstract fun bindScheduleRepository(
        impl: FakeScheduleRepository,
    ): ScheduleRepository
}
