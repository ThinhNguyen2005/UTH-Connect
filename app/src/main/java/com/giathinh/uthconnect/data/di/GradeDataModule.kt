package com.giathinh.uthconnect.data.di

import com.giathinh.uthconnect.data.repository.SupabaseStudentGradeRepository
import com.giathinh.uthconnect.domain.repository.StudentGradeRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class GradeDataModule {

    @Binds
    @Singleton
    abstract fun bindStudentGradeRepository(
        impl: SupabaseStudentGradeRepository,
    ): StudentGradeRepository
}
