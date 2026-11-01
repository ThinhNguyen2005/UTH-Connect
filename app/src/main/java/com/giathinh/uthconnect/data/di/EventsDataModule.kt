package com.giathinh.uthconnect.data.di

import com.giathinh.uthconnect.data.repository.StaticEventRepository
import com.giathinh.uthconnect.data.repository.DemoStudentIdProvider
import com.giathinh.uthconnect.domain.repository.EventRepository
import com.giathinh.uthconnect.domain.repository.StudentIdProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class EventsDataModule {
    @Binds
    abstract fun bindEventRepository(repository: StaticEventRepository): EventRepository

    @Binds
    abstract fun bindStudentIdProvider(repository: DemoStudentIdProvider): StudentIdProvider
}
