package com.giathinh.uthconnect.data.di

import com.giathinh.uthconnect.data.repository.SupabaseAnnouncementRepository
import com.giathinh.uthconnect.domain.repository.AnnouncementRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Binding tầng `data` của tab Trang chủ: `AnnouncementRepository` (interface ở domain)
 * → `SupabaseAnnouncementRepository`.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class HomeDataModule {

    @Binds
    abstract fun bindAnnouncementRepository(
        implementation: SupabaseAnnouncementRepository,
    ): AnnouncementRepository
}
