package com.giathinh.uthconnect.presentation.utilities.settings

/**
 * Trạng thái cài đặt của ứng dụng.
 */
data class SettingsUiState(
    val tuitionReminderEnabled: Boolean = true,
    val trainingPointNotifEnabled: Boolean = true,
    val eventNotifEnabled: Boolean = false,
    val appVersion: String = "1.0.0",
    val buildNumber: String = "Build 2026.1",
    val developer: String = "Nhóm dự án UTH Connect",
)
