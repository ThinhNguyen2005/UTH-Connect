package com.giathinh.uthconnect.data.di

import com.giathinh.uthconnect.data.repository.FirebaseAuthRepository
import com.giathinh.uthconnect.data.repository.FirebaseAuthSource
import com.giathinh.uthconnect.data.repository.SdkFirebaseAuthSource
import com.giathinh.uthconnect.data.repository.SessionPreferences
import com.giathinh.uthconnect.data.repository.SharedSessionPreferences
import com.giathinh.uthconnect.data.repository.PreferencesSessionRepository
import com.giathinh.uthconnect.domain.repository.AuthRepository
import com.giathinh.uthconnect.domain.repository.SessionRepository
import com.giathinh.uthconnect.domain.repository.StudentContactRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import javax.inject.Provider
import javax.inject.Singleton
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class AuthDataModule {
    @Binds abstract fun bindPreferences(implementation: SharedSessionPreferences): SessionPreferences
    @Binds abstract fun bindSource(implementation: SdkFirebaseAuthSource): FirebaseAuthSource
    @Binds abstract fun bindSession(implementation: PreferencesSessionRepository): SessionRepository
    @Binds abstract fun bindContacts(implementation: FirebaseAuthRepository): StudentContactRepository
    companion object {
        @Provides
        @Singleton
        fun provideAuth(firebase: Provider<FirebaseAuthRepository>): AuthRepository = firebase.get()
    }
}
