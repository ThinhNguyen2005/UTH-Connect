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

    /** Provides the repository for the active backend. */
    @Provides
    @Singleton
    fun provideLostFoundRepository(
        inMemoryRepo: Provider<InMemoryLostFoundRepository>,
        supabaseRepo: Provider<SupabaseLostFoundRepository>,
    ): LostFoundRepository = when {
        BuildConfig.USE_SUPABASE -> supabaseRepo.get()
        else -> inMemoryRepo.get()
    }
}
