package com.giathinh.uthconnect.data.di

import com.giathinh.uthconnect.BuildConfig
import com.giathinh.uthconnect.data.repository.SupabaseBookListingRepository
import com.giathinh.uthconnect.data.repository.UnavailableBookListingRepository
import com.giathinh.uthconnect.domain.repository.BookListingRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Provider
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object BooksDataModule {
    @Provides
    @Singleton
    fun provideBookListingRepository(
        supabase: Provider<SupabaseBookListingRepository>,
        unavailable: Provider<UnavailableBookListingRepository>,
    ): BookListingRepository = if (BuildConfig.USE_MOCK_DATA) unavailable.get() else supabase.get()
}
