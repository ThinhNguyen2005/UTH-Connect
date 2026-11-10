package com.giathinh.uthconnect

import com.giathinh.uthconnect.domain.repository.SettingsRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Cửa lấy dependency **trước khi** Hilt kịp injection activity.
 *
 * `MainActivity.attachBaseContext` chạy trước `onCreate`, tức trước lúc Hilt nạp các
 * `@Inject lateinit var` — mà Locale thì phải áp đúng ngay từ lần dựng tài nguyên đầu tiên.
 * [EntryPoint] là đường chính thức cho chỗ không phải component do Hilt sinh ra;
 * `SingletonComponent` đã sẵn sàng vì `Application.onCreate` chạy trước activity đầu tiên.
 */
@InstallIn(SingletonComponent::class)
@EntryPoint
internal interface SettingsEntryPoint {
    fun settingsRepository(): SettingsRepository
}
