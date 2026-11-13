package com.giathinh.uthconnect.data.di

import com.giathinh.uthconnect.data.repository.DataStoreHousingDraftStore
import com.giathinh.uthconnect.data.repository.DataStoreHousingImageCleanupStore
import com.giathinh.uthconnect.data.repository.SupabaseHousingRepository
import com.giathinh.uthconnect.domain.repository.HousingDraftStore
import com.giathinh.uthconnect.domain.repository.HousingImageCleanupStore
import com.giathinh.uthconnect.domain.repository.HousingRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class HousingDataModule {

    @Binds
    @Singleton
    abstract fun bindHousingRepository(
        impl: SupabaseHousingRepository,
    ): HousingRepository

    @Binds
    @Singleton
    abstract fun bindHousingDraftStore(
        impl: DataStoreHousingDraftStore,
    ): HousingDraftStore

    @Binds
    @Singleton
    abstract fun bindHousingImageCleanupStore(
        impl: DataStoreHousingImageCleanupStore,
    ): HousingImageCleanupStore
}
