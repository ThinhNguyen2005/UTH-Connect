package com.giathinh.uthconnect

import android.app.Application
import com.giathinh.uthconnect.BuildConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.HiltAndroidApp

/**
 * Application duy nhất của app và là **gốc của cây DI**.
 *
 * `@HiltAndroidApp` sinh `SingletonComponent` — component cha của mọi component
 * khác (Activity, ViewModel…). Không có nó, Hilt không injection chỗ nào cả.
 *
 * File thuộc quyền sở hữu chung (xem `docs/REPO_TREE.md` mục "file gốc"): chỉ sửa khi
 * thật sự thêm khởi tạo cấp app (WorkManager, crash report…), không thêm binding
 * nghiệp vụ vào đây — binding của module nào nằm trong `data/di/` của module đó.
 */
@HiltAndroidApp
class UTHConnectApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.USE_FIREBASE_EMULATOR) {
            val projectId = BuildConfig.FIREBASE_EMULATOR_PROJECT_ID
            FirebaseApp.initializeApp(this, FirebaseOptions.Builder()
                .setApplicationId("1:123456789000:android:uthconnectqaemulator")
                .setApiKey("emulator-only-api-key")
                .setProjectId(projectId)
                .setStorageBucket("$projectId.appspot.com")
                .build())
            FirebaseAuth.getInstance().useEmulator(BuildConfig.FIREBASE_EMULATOR_HOST, 9099)
            FirebaseFirestore.getInstance().useEmulator(BuildConfig.FIREBASE_EMULATOR_HOST, 8080)
            FirebaseStorage.getInstance().useEmulator(BuildConfig.FIREBASE_EMULATOR_HOST, 9199)
        }
    }
}
