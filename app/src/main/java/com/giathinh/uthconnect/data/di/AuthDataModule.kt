package com.giathinh.uthconnect.data.di

import android.content.Context
import com.giathinh.uthconnect.BuildConfig
import com.giathinh.uthconnect.data.repository.DemoAccountStore
import com.giathinh.uthconnect.data.repository.DemoAuthRepository
import com.giathinh.uthconnect.data.repository.FirebaseAuthRepository
import com.giathinh.uthconnect.data.repository.PreferencesSessionRepository
import com.giathinh.uthconnect.data.repository.SharedPreferencesDemoAccountStore
import com.giathinh.uthconnect.data.repository.SupabaseAuthRepository
import com.giathinh.uthconnect.domain.repository.AuthRepository
import com.giathinh.uthconnect.domain.repository.SessionRepository
import com.giathinh.uthconnect.domain.repository.StudentContactRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Provider
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AuthDataModule {
    @Binds abstract fun bindSession(implementation: PreferencesSessionRepository): SessionRepository

    companion object {
        @Provides
        @Singleton
        fun provideDemoAccountStore(@ApplicationContext context: Context): DemoAccountStore = SharedPreferencesDemoAccountStore(
            context.getSharedPreferences("uth_auth_demo_account", Context.MODE_PRIVATE),
        )

        @Provides
        @Singleton
        fun provideAuth(
            demo: Provider<DemoAuthRepository>,
            firebase: Provider<FirebaseAuthRepository>,
            supabase: Provider<SupabaseAuthRepository>,
        ): AuthRepository = when {
            BuildConfig.USE_SUPABASE -> supabase.get()
            BuildConfig.USE_MOCK_DATA -> demo.get()
            else -> firebase.get()
        }

        @Provides
        @Singleton
        fun provideContacts(
            demo: Provider<DemoAuthRepository>,
            supabase: Provider<SupabaseAuthRepository>,
        ): StudentContactRepository = when {
            BuildConfig.USE_SUPABASE -> supabase.get()
            else -> demo.get()
        }
    }
}
