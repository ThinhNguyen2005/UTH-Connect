package com.giathinh.uthconnect.data.di

import com.giathinh.uthconnect.data.repository.FakeNotificationRepository
import com.giathinh.uthconnect.domain.repository.NotificationRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class NotificationDataModule {

    @Binds
    @Singleton
    abstract fun bindNotificationRepository(
        impl: FakeNotificationRepository,
    ): NotificationRepository
}
