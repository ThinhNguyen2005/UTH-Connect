package com.giathinh.uthconnect.data.di

import com.giathinh.uthconnect.data.repository.SupabaseIncidentRepository
import com.giathinh.uthconnect.domain.repository.IncidentRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class IncidentDataModule {
    @Binds
    @Singleton
    abstract fun bindIncidentRepository(impl: SupabaseIncidentRepository): IncidentRepository
}
