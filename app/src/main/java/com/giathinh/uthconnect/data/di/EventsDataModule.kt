package com.giathinh.uthconnect.data.di

import com.giathinh.uthconnect.data.repository.SupabaseEventRepository
import com.giathinh.uthconnect.domain.repository.EventRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object EventsDataModule {
    @Provides @Singleton
    fun provideEventRepository(
        supabase: SupabaseEventRepository,
    ): EventRepository = supabase
}
