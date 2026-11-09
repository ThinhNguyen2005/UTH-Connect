package com.giathinh.uthconnect.data.di

import com.giathinh.uthconnect.BuildConfig
import com.giathinh.uthconnect.data.repository.StaticAnnouncementRepository
import com.giathinh.uthconnect.data.repository.SupabaseAnnouncementRepository
import com.giathinh.uthconnect.domain.repository.AnnouncementRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Provider
import javax.inject.Singleton

/** Selects the announcement source for the configured backend. */
@Module
@InstallIn(SingletonComponent::class)
object HomeDataModule {
    /** Provides one announcement repository for the active backend. */
    @Provides
    @Singleton
    fun provideAnnouncementRepository(
        static: Provider<StaticAnnouncementRepository>,
        supabase: Provider<SupabaseAnnouncementRepository>,
    ): AnnouncementRepository = if (BuildConfig.USE_SUPABASE) supabase.get() else static.get()
}
