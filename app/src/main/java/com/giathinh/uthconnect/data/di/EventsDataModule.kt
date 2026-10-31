package com.giathinh.uthconnect.data.di

import com.giathinh.uthconnect.data.repository.StaticEventRepository
import com.giathinh.uthconnect.domain.repository.EventRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class EventsDataModule {
    @Binds
    abstract fun bindEventRepository(repository: StaticEventRepository): EventRepository
}
