package com.giathinh.uthconnect

import android.app.Application
import com.giathinh.uthconnect.data.repository.CrashIdentity
import dagger.hilt.android.HiltAndroidApp

/**
 * Application class gắn `@HiltAndroidApp` để kích hoạt cây DI Hilt.
 *
 * Chỉ dùng cho khởi tạo toàn app (DI, log tree, push SDK khi cần). Không nhét logic
 * nghiệp vụ vào đây — binding của module nào nằm trong `data/di/` của module đó.
 */
@HiltAndroidApp
class UTHConnectApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        CrashIdentity.clear()
    }
}
