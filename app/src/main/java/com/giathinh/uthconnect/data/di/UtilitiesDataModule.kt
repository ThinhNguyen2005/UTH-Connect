package com.giathinh.uthconnect.data.di

import com.giathinh.uthconnect.data.repository.PreferencesSettingsRepository
import com.giathinh.uthconnect.data.repository.StaticUtilityModuleRepository
import com.giathinh.uthconnect.data.repository.UnavailableTrainingPointRepository
import com.giathinh.uthconnect.data.repository.UnavailableTuitionRepository
import com.giathinh.uthconnect.domain.repository.SettingsRepository
import com.giathinh.uthconnect.domain.repository.TrainingPointRepository
import com.giathinh.uthconnect.domain.repository.TuitionRepository
import com.giathinh.uthconnect.domain.repository.UtilityModuleRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class UtilitiesDataModule {

    @Binds
    abstract fun bindUtilityModuleRepository(
        implementation: StaticUtilityModuleRepository,
    ): UtilityModuleRepository

    @Binds
    abstract fun bindTuitionRepository(
        implementation: UnavailableTuitionRepository,
    ): TuitionRepository

    @Binds
    abstract fun bindTrainingPointRepository(
        implementation: UnavailableTrainingPointRepository,
    ): TrainingPointRepository

    @Binds
    abstract fun bindSettingsRepository(
        implementation: PreferencesSettingsRepository,
    ): SettingsRepository
}
