package com.giathinh.uthconnect.data.di

import com.giathinh.uthconnect.data.repository.SupabasePersonalNotificationRepository
import com.giathinh.uthconnect.domain.repository.UserNotificationRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class UserNotificationDataModule {
    @Binds
    @Singleton
    abstract fun bindUserNotificationRepository(
        impl: SupabasePersonalNotificationRepository,
    ): UserNotificationRepository
}
