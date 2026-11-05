package com.giathinh.uthconnect.presentation.utilities.settings

import com.giathinh.uthconnect.domain.model.LANGUAGE_TAG_VIETNAMESE
import com.giathinh.uthconnect.domain.model.ThemeMode

/**
 * Trạng thái cài đặt của ứng dụng.
 *
 * `languageTag` là BCP-47 ("vi"/"en") — đúng dạng đã lưu, không phải nhãn "VI"/"EN"
 * hiển thị trên nút; Screen tự map tag sang nhãn đã dịch.
 */
data class SettingsUiState(
    val tuitionReminderEnabled: Boolean = true,
    val trainingPointNotifEnabled: Boolean = true,
    val eventNotifEnabled: Boolean = false,
    val languageTag: String = LANGUAGE_TAG_VIETNAMESE,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val appVersion: String = "1.0.0",
    val buildNumber: String = "Build 2026.1",
    val developer: String = "Nhóm dự án UTH Connect",
)
