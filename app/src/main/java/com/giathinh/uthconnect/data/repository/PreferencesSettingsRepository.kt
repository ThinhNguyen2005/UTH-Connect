package com.giathinh.uthconnect.data.repository

import android.content.Context
import com.giathinh.uthconnect.domain.model.AppSettings
import com.giathinh.uthconnect.domain.model.LANGUAGE_TAG_VIETNAMESE
import com.giathinh.uthconnect.domain.model.ThemeMode
import com.giathinh.uthconnect.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Cài đặt lưu cục bộ trong `SharedPreferences` — chưa có backend cho preference,
 * nên mọi giá trị phải đọc được ngay khi app khởi động (không chờ network).
 */
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

    override suspend fun setLanguageTag(languageTag: String) {
        prefs.edit().putString(KEY_LANGUAGE_TAG, languageTag).apply()
        _settings.update { it.copy(languageTag = languageTag) }
    }

    override suspend fun setThemeMode(themeMode: ThemeMode) {
        prefs.edit().putString(KEY_THEME_MODE, themeMode.name).apply()
        _settings.update { it.copy(themeMode = themeMode) }
    }

    override fun currentLanguageTag(): String = _settings.value.languageTag

    private fun loadSettings(): AppSettings = AppSettings(
        tuitionReminderEnabled = prefs.getBoolean(KEY_TUITION_REMINDER, true),
        trainingPointNotifEnabled = prefs.getBoolean(KEY_TRAINING_NOTIF, true),
        languageTag = prefs.getString(KEY_LANGUAGE_TAG, LANGUAGE_TAG_VIETNAMESE)
            ?: LANGUAGE_TAG_VIETNAMESE,
        themeMode = readThemeMode(),
        appVersion = "1.0.0",
        buildNumber = "Build 2026.1",
        developer = "Nhóm dự án UTH Connect",
    )

    /** Giá trị lạ (do lần phát hành cũ để lại) rơi về [ThemeMode.SYSTEM], không crash app. */
    private fun readThemeMode(): ThemeMode {
        val stored = prefs.getString(KEY_THEME_MODE, null) ?: return ThemeMode.SYSTEM
        return ThemeMode.entries.firstOrNull { it.name == stored } ?: ThemeMode.SYSTEM
    }

    companion object {
        private const val PREFS_NAME = "uth_connect_settings"
        private const val KEY_TUITION_REMINDER = "key_tuition_reminder"
        private const val KEY_TRAINING_NOTIF = "key_training_notif"
        private const val KEY_LANGUAGE_TAG = "key_language_tag"
        private const val KEY_THEME_MODE = "key_theme_mode"
    }
}
