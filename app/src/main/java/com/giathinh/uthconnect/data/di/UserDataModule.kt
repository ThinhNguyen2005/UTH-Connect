package com.giathinh.uthconnect.data.di

import com.giathinh.uthconnect.data.repository.SupabaseUserRepository
import com.giathinh.uthconnect.domain.repository.UserRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class UserDataModule {

    @Binds
    @Singleton
    abstract fun bindUserRepository(
        impl: SupabaseUserRepository,
    ): UserRepository
}
