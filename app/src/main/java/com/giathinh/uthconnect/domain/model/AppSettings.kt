package com.giathinh.uthconnect.domain.model

data class AppSettings(
    val tuitionReminderEnabled: Boolean = true,
    val trainingPointNotifEnabled: Boolean = true,
    val appVersion: String = "1.0.0",
    val buildNumber: String = "Build 2026.1",
    val developer: String = "Nhóm dự án UTH Connect",
)
