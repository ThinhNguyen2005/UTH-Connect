package com.giathinh.uthconnect

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application duy nhất của app và là **gốc của cây DI**.
 *
 * `@HiltAndroidApp` sinh `SingletonComponent` — component cha của mọi component
 * khác (Activity, ViewModel…). Không có nó, Hilt không injection chỗ nào cả.
 *
 * File thuộc quyền sở hữu chung (xem `REPO_TREE.md` mục "file gốc"): chỉ sửa khi
 * thật sự thêm khởi tạo cấp app (WorkManager, crash report…), không thêm binding
 * nghiệp vụ vào đây — binding của module nào nằm trong `data/di/` của module đó.
 */
@HiltAndroidApp
class UTHConnectApplication : Application()
