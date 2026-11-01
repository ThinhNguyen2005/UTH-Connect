package com.giathinh.uthconnect.data.di

import com.giathinh.uthconnect.data.repository.FirestoreEventRepository
import com.giathinh.uthconnect.domain.repository.EventRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class EventsDataModule {

    @Binds
    @Singleton
    abstract fun bindEventRepository(
        implementation: FirestoreEventRepository,
    ): EventRepository
}
