package com.giathinh.uthconnect.data.repository

import android.content.Context
import com.giathinh.uthconnect.domain.model.AppSettings
import com.giathinh.uthconnect.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferencesSettingsRepository @Inject constructor(
    @ApplicationContext context: Context,
) : SettingsRepository {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettings())

    override fun getSettings(): Flow<AppSettings> = _settings.asStateFlow()

    override suspend fun updateTuitionReminder(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_TUITION_REMINDER, enabled).apply()
        _settings.update { it.copy(tuitionReminderEnabled = enabled) }
    }

    override suspend fun updateTrainingPointNotif(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_TRAINING_NOTIF, enabled).apply()
        _settings.update { it.copy(trainingPointNotifEnabled = enabled) }
    }

    private fun loadSettings(): AppSettings {
        return AppSettings(
            tuitionReminderEnabled = prefs.getBoolean(KEY_TUITION_REMINDER, true),
            trainingPointNotifEnabled = prefs.getBoolean(KEY_TRAINING_NOTIF, true),
            appVersion = "1.0.0",
            buildNumber = "Build 2026.1",
            developer = "Nhóm dự án UTH Connect",
        )
    }

    companion object {
        private const val PREFS_NAME = "uth_connect_settings"
        private const val KEY_TUITION_REMINDER = "key_tuition_reminder"
        private const val KEY_TRAINING_NOTIF = "key_training_notif"
    }
}
