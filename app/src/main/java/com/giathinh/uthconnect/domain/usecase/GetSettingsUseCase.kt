package com.giathinh.uthconnect.domain.usecase

import com.giathinh.uthconnect.domain.model.AppSettings
import com.giathinh.uthconnect.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetSettingsUseCase @Inject constructor(
    private val repository: SettingsRepository,
) {
    operator fun invoke(): Flow<AppSettings> = repository.getSettings()
}
