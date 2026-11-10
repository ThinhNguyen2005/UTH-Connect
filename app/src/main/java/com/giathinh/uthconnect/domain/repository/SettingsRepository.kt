package com.giathinh.uthconnect.domain.repository

import com.giathinh.uthconnect.domain.model.AppSettings
import com.giathinh.uthconnect.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun getSettings(): Flow<AppSettings>
    suspend fun updateTuitionReminder(enabled: Boolean)
    suspend fun updateTrainingPointNotif(enabled: Boolean)
    suspend fun setLanguageTag(languageTag: String)
    suspend fun setThemeMode(themeMode: ThemeMode)

    /**
     * Ngôn ngữ đang bật, đọc **đồng bộ**.
     *
     * `Activity.attachBaseContext` chạy trước khi kịp collect bất kỳ flow nào, nên
     * Locale bắt buộc phải lấy được ngay; repository đã cầm `SharedPreferences` trong
     * bộ nhớ nên đây là phép đọc trong bộ nhớ, không phải I/O chặn.
     */
    fun currentLanguageTag(): String
}
