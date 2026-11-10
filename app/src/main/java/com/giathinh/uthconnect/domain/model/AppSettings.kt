package com.giathinh.uthconnect.domain.model

// Mã ngôn ngữ BCP-47 app hỗ trợ — nguồn sự thật duy nhất cho mọi chỗ đọc/ghi
// `AppSettings.languageTag`. `res/xml/locales_config.xml` khai đúng hai mã này.
const val LANGUAGE_TAG_VIETNAMESE = "vi"
const val LANGUAGE_TAG_ENGLISH = "en"

/**
 * Chế độ giao diện người dùng chọn trong Cài đặt.
 *
 * [SYSTEM] không phải giá trị dự phòng: mặc định của app là đi theo hệ thống để
 * người dùng đã bật Dark mode trong Android không bị phá thói quen.
 */
enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
    ;

    /**
     * Quyết định sáng/tối cuối cùng mà shell của app phải render.
     *
     * Đặt ở `domain` vì `core/designsystem` bị cấm import `domain`
     * (`docs/CODING_STANDARDS.md` mục 3.1), còn hàm này thuần Kotlin, không chạm Compose.
     * [SYSTEM] nhường cho hệ thống; hai mode kia là ý người dùng trong Cài đặt và thắng.
     */
    fun resolveDarkTheme(systemInDarkTheme: Boolean): Boolean = when (this) {
        SYSTEM -> systemInDarkTheme
        LIGHT -> false
        DARK -> true
    }
}

/**
 * Cài đặt chung của app, lưu trong SharedPreferences (`PreferencesSettingsRepository`).
 *
 * `languageTag` là BCP-47 viết thường ("vi", "en") — đúng dạng `Locale` cần trong
 * `Configuration.setLocale`, không phải mã hiển thị "VI"/"EN" trên nút.
 */
data class AppSettings(
    val tuitionReminderEnabled: Boolean = true,
    val trainingPointNotifEnabled: Boolean = true,
    val languageTag: String = LANGUAGE_TAG_VIETNAMESE,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val appVersion: String = "1.0.0",
    val buildNumber: String = "Build 2026.1",
    val developer: String = "Nhóm dự án UTH Connect",
)
