package com.giathinh.uthconnect.data.di

import com.giathinh.uthconnect.BuildConfig
import com.giathinh.uthconnect.data.repository.DemoAuthRepository
import com.giathinh.uthconnect.data.repository.FirebaseAuthRepository
import com.giathinh.uthconnect.data.repository.PreferencesSessionRepository
import com.giathinh.uthconnect.domain.repository.AuthRepository
import com.giathinh.uthconnect.domain.repository.SessionRepository
import com.giathinh.uthconnect.domain.repository.StudentContactRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Provider
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AuthDataModule {
    @Binds abstract fun bindSession(implementation: PreferencesSessionRepository): SessionRepository
    @Binds abstract fun bindContacts(implementation: DemoAuthRepository): StudentContactRepository

    companion object {
        @Provides
        @Singleton
        fun provideAuth(
            demo: Provider<DemoAuthRepository>,
            firebase: Provider<FirebaseAuthRepository>,
        ): AuthRepository = if (BuildConfig.USE_MOCK_DATA) demo.get() else firebase.get()
    }
}
