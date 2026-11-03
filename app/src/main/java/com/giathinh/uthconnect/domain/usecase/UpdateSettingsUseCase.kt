package com.giathinh.uthconnect.domain.usecase

import com.giathinh.uthconnect.domain.repository.SettingsRepository
import javax.inject.Inject

class UpdateSettingsUseCase @Inject constructor(
    private val repository: SettingsRepository,
) {
    suspend fun setTuitionReminder(enabled: Boolean) = repository.updateTuitionReminder(enabled)
    suspend fun setTrainingPointNotif(enabled: Boolean) = repository.updateTrainingPointNotif(enabled)
}
