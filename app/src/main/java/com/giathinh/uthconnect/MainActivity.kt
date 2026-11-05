package com.giathinh.uthconnect

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.giathinh.uthconnect.core.designsystem.theme.UTHConnectTheme
import com.giathinh.uthconnect.domain.model.ThemeMode
import com.giathinh.uthconnect.domain.repository.SettingsRepository
import com.giathinh.uthconnect.presentation.navigation.UthApp
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.launch
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
        enableEdgeToEdge()
        observeThemeMode()
        setContent {
            UTHConnectTheme(darkTheme = themeMode.resolveDarkTheme(isSystemInDarkTheme())) {
                UthApp()
            }
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
}
