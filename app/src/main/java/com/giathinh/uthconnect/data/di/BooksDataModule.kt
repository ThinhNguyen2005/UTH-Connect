package com.giathinh.uthconnect.data.di

import com.giathinh.uthconnect.data.repository.SupabaseBookListingRepository
import com.giathinh.uthconnect.domain.repository.BookListingRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class BooksDataModule {
    @Binds
    @Singleton
    abstract fun bindBookListingRepository(
        impl: SupabaseBookListingRepository,
    ): BookListingRepository
}
