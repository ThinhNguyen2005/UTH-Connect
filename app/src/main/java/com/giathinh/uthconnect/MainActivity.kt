package com.giathinh.uthconnect

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.giathinh.uthconnect.core.designsystem.theme.UTHConnectTheme
import com.giathinh.uthconnect.domain.model.ThemeMode
import com.giathinh.uthconnect.domain.repository.SettingsRepository
import com.giathinh.uthconnect.presentation.navigation.UthApp
import com.giathinh.uthconnect.data.push.PushIntentContract
import com.google.firebase.FirebaseApp
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigFetchThrottledException
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Locale
import javax.inject.Inject

/**
 * Single activity duy nhất của app. Mọi màn hình là Composable nằm trong
 * `NavHost` của [UthApp]; activity không chứa logic nghiệp vụ.
 *
 * `@AndroidEntryPoint` biến activity này thành **điểm vào DI duy nhất** của cả
 * cây Compose: Hilt thay `ViewModelProvider.Factory` mặc định bằng factory của nó,
 * nên `hiltViewModel()` trong các `…Route.kt` lấy được ViewModel mà không cần
 * truyền container thủ công.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    /**
     * Chế độ giao diện người dùng chọn trong Cài đặt. `by mutableStateOf` để đổi mode
     * là recompose ngay, không cần dựng lại activity.
     */
    private var themeMode by mutableStateOf(ThemeMode.SYSTEM)
    private var pushNotificationId by mutableStateOf<String?>(null)
    private var appAvailability by mutableStateOf<AppAvailability?>(null)

    /**
     * Áp ngôn ngữ đã lưu **trước** khi dựng tài nguyên.
     *
     * Không dùng field `@Inject` ở đây vì Hilt injection chạy trong `onCreate`, tức là
     * sau `attachBaseContext`; [SettingsEntryPoint] là đường chính thức cho chỗ đó.
     * `applyOverrideConfiguration` (không phải `createConfigurationContext`) giữ nguyên
     * base context của activity nên window/insets/theme của app không đổi hành vi.
     */
    override fun attachBaseContext(newBase: Context) {
        val languageTag = EntryPointAccessors
            .fromApplication(newBase.applicationContext, SettingsEntryPoint::class.java)
            .settingsRepository()
            .currentLanguageTag()
        val configuration = Configuration(newBase.resources.configuration).apply {
            setLocale(Locale(languageTag))
        }
        applyOverrideConfiguration(configuration)
        super.attachBaseContext(newBase)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createNotificationChannel()
        pushNotificationId = intent?.getStringExtra(PushIntentContract.NOTIFICATION_ID)
        enableEdgeToEdge()
        observeThemeMode()
        observeRemoteConfig()
        setContent {
            UTHConnectTheme(darkTheme = themeMode.resolveDarkTheme(isSystemInDarkTheme())) {
                when (val state = appAvailability) {
                    null -> Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }

                    AppAvailability.AVAILABLE -> UthApp(
                        pushNotificationId = pushNotificationId,
                        onPushNavigationHandled = { id ->
                            if (pushNotificationId == id) {
                                pushNotificationId = null
                                intent?.removeExtra(PushIntentContract.NOTIFICATION_ID)
                            }
                        },
                    )

                    else -> AvailabilityScreen(state)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pushNotificationId = intent.getStringExtra(PushIntentContract.NOTIFICATION_ID)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(
                    PushIntentContract.CHANNEL_ID,
                    getString(R.string.notification_channel_updates),
                    NotificationManager.IMPORTANCE_HIGH,
                ).apply { description = getString(R.string.notification_channel_updates_description) },
            )
        }
    }

    /**
     * Tải trạng thái gate đã lưu bền vững, sau đó thực thi fetch & activate Remote Config
     * trên luồng I/O ([Dispatchers.IO]) để tránh chặn UI Thread.
     *
     * Khi fetch thất bại hoặc trả về giá trị mặc định, trạng thái gate đã lưu được ưu tiên giữ nguyên
     * để duy trì việc chặn truy cập đối với phiên bản cũ hoặc ứng dụng đang bảo trì.
     */
    private fun observeRemoteConfig() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                if (FirebaseApp.getApps(this@MainActivity).isEmpty()) {
                    appAvailability = AppAvailability.AVAILABLE
                    return@repeatOnLifecycle
                }
                val saved = withContext(Dispatchers.IO) { loadSavedGate(this@MainActivity) }
                val (initialState, _) = resolveGate(saved, null, null, BuildConfig.VERSION_NAME)
                appAvailability = initialState

                try {
                    withContext(Dispatchers.IO) {
                        updateRemoteConfigGate(saved)
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: FirebaseRemoteConfigFetchThrottledException) {
                    if (appAvailability == null) {
                        appAvailability = initialState
                    }
                } catch (_: Exception) {
                    if (appAvailability == null) {
                        appAvailability = initialState
                    }
                }
            }
        }
    }

    private suspend fun updateRemoteConfigGate(saved: SavedGate) {
        val config = FirebaseRemoteConfig.getInstance()
        val fetchIntervalSeconds = if (BuildConfig.DEBUG) 0L else RELEASE_FETCH_INTERVAL_SECONDS
        val settings = FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(fetchIntervalSeconds)
            .build()
        config.setConfigSettingsAsync(settings).await()
        config.setDefaultsAsync(
            mapOf("force_update_min_version" to "1.0.0", "maintenance_mode" to false),
        ).await()

        config.fetchAndActivate().await()

        val maintVal = config.getValue("maintenance_mode")
        val minVerVal = config.getValue("force_update_min_version")

        val remoteMaint = if (maintVal.source == FirebaseRemoteConfig.VALUE_SOURCE_REMOTE) {
            maintVal.asBoolean()
        } else {
            null
        }
        val remoteMinVer = if (minVerVal.source == FirebaseRemoteConfig.VALUE_SOURCE_REMOTE) {
            minVerVal.asString()
        } else {
            null
        }

        val (newState, newSaved) = resolveGate(
            saved = saved,
            remoteMaintenance = remoteMaint,
            remoteMinVersion = remoteMinVer,
            currentVersion = BuildConfig.VERSION_NAME,
        )
        saveGate(this@MainActivity, newSaved)

        withContext(Dispatchers.Main) {
            appAvailability = newState
        }
    }

    private fun observeThemeMode() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                settingsRepository.getSettings().collect { settings ->
                    themeMode = settings.themeMode
                }
            }
        }
    }

    companion object {
        private const val RELEASE_FETCH_INTERVAL_SECONDS = 3600L
    }
}
