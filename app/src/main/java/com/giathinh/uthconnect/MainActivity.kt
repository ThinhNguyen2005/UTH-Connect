package com.giathinh.uthconnect

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.giathinh.uthconnect.core.designsystem.theme.UTHConnectTheme
import com.giathinh.uthconnect.presentation.navigation.UthApp
import dagger.hilt.android.AndroidEntryPoint

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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            UTHConnectTheme {
                UthApp()
            }
        }
    }
}
