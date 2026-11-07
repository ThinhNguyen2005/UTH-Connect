package com.giathinh.uthconnect.data.di

import com.giathinh.uthconnect.data.repository.SupabaseTicketRepository
import com.giathinh.uthconnect.domain.repository.TicketRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object TicketDataModule {
    @Provides @Singleton
    fun provideTicketRepository(
        supabase: SupabaseTicketRepository,
    ): TicketRepository = supabase
}
