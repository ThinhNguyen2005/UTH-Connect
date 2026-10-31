package com.giathinh.uthconnect.data.di

import com.giathinh.uthconnect.data.repository.DemoAuthRepository
import com.giathinh.uthconnect.data.repository.PreferencesSessionRepository
import com.giathinh.uthconnect.domain.repository.AuthRepository
import com.giathinh.uthconnect.domain.repository.SessionRepository
import com.giathinh.uthconnect.domain.repository.StudentContactRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class AuthDataModule {
    @Binds abstract fun bindAuth(implementation: DemoAuthRepository): AuthRepository
    @Binds abstract fun bindSession(implementation: PreferencesSessionRepository): SessionRepository
    @Binds abstract fun bindContacts(implementation: DemoAuthRepository): StudentContactRepository
}
