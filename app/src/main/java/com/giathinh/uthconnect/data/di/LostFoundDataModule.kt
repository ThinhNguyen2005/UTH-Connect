package com.giathinh.uthconnect.data.di

import com.giathinh.uthconnect.BuildConfig
import com.giathinh.uthconnect.data.repository.InMemoryLostFoundRepository
import com.giathinh.uthconnect.data.repository.SupabaseLostFoundRepository
import com.giathinh.uthconnect.domain.repository.LostFoundRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Provider
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object LostFoundDataModule {

    @Provides
    @Singleton
    fun provideLostFoundRepository(
        inMemory: Provider<InMemoryLostFoundRepository>,
        supabase: Provider<SupabaseLostFoundRepository>,
    ): LostFoundRepository = if (BuildConfig.USE_SUPABASE) supabase.get() else inMemory.get()
}
