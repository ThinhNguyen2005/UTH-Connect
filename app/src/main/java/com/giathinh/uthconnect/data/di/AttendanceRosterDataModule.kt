package com.giathinh.uthconnect.data.di

import com.giathinh.uthconnect.data.repository.SupabaseAttendanceRosterRepository
import com.giathinh.uthconnect.domain.repository.AttendanceRosterRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class AttendanceRosterDataModule {
    @Binds abstract fun bindRosterRepository(implementation: SupabaseAttendanceRosterRepository): AttendanceRosterRepository
}
