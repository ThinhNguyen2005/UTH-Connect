package com.giathinh.uthconnect.data.di

import com.giathinh.uthconnect.data.repository.MockTicketRepository
import com.giathinh.uthconnect.domain.repository.TicketRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Binding repository thuộc riêng feature vé. */
@Module
@InstallIn(SingletonComponent::class)
abstract class TicketDataModule {
    @Binds
    abstract fun bindTicketRepository(repository: MockTicketRepository): TicketRepository
}
