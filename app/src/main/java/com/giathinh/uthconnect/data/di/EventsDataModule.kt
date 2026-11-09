package com.giathinh.uthconnect.data.di

import com.giathinh.uthconnect.BuildConfig
import com.giathinh.uthconnect.data.repository.FirestoreEventRepository
import com.giathinh.uthconnect.data.repository.SupabaseEventRepository
import com.giathinh.uthconnect.domain.repository.EventRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Provider
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object EventsDataModule {
    @Provides @Singleton
    fun provideEventRepository(
        firestore: Provider<FirestoreEventRepository>,
        supabase: Provider<SupabaseEventRepository>,
    ): EventRepository = if (BuildConfig.USE_SUPABASE) supabase.get() else firestore.get()
}
