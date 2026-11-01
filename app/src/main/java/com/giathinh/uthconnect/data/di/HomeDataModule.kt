package com.giathinh.uthconnect.data.di

import com.giathinh.uthconnect.data.repository.StaticAnnouncementRepository
import com.giathinh.uthconnect.domain.repository.AnnouncementRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Binding tầng `data` của tab Trang chủ: `AnnouncementRepository` (interface ở domain)
 * → `StaticAnnouncementRepository` (impl tạm thời).
 *
 * `@Singleton` đặt trên implementation, không lặp lại ở hàm bind. Khi có Firestore, chỉ
 * thay class ở tham số dưới đây — ViewModel/UseCase không phải sửa.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class HomeDataModule {

    @Binds
    abstract fun bindAnnouncementRepository(
        implementation: StaticAnnouncementRepository,
    ): AnnouncementRepository
}
