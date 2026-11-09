package com.giathinh.uthconnect.data.di

import com.giathinh.uthconnect.data.repository.InMemoryLostFoundRepository
import com.giathinh.uthconnect.domain.repository.LostFoundRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class LostFoundDataModule {

    @Binds
    @Singleton
    abstract fun bindLostFoundRepository(
        impl: InMemoryLostFoundRepository,
    ): LostFoundRepository
}
