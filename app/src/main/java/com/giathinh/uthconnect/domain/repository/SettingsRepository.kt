package com.giathinh.uthconnect.domain.repository

import com.giathinh.uthconnect.domain.model.AppSettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun getSettings(): Flow<AppSettings>
    suspend fun updateTuitionReminder(enabled: Boolean)
    suspend fun updateTrainingPointNotif(enabled: Boolean)
}
