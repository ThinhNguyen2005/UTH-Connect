package com.giathinh.uthconnect.presentation.utilities

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Tầng nối (route) của tab Tiện ích: chỗ **duy nhất** trong module lấy ViewModel ra
 * khỏi Hilt.
 *
 * Tách khỏi `UtilitiesScreen` vì:
 * - Screen nhận `UiState` + phát `Action` thì preview được mọi trạng thái mà không
 *   cần repository thật (điều kiện DoD trong `README.md` mục 10.2).
 * - ViewModel không được truyền xuống composable con — chỉ truyền dữ liệu và hàm
 *   (theo tài liệu Android: "access ViewModels at screen-level composables").
 *
 * `hiltViewModel()` scope ViewModel theo `NavBackStackEntry` của route (NavHost đặt
 * `LocalViewModelStoreOwner` theo entry), nên trở lại tab là gặp lại đúng trạng thái
 * cũ; activity gốc đã có `@AndroidEntryPoint` nên không cần factory thủ công.
 */
@Composable
fun UtilitiesRoute(
    modifier: Modifier = Modifier,
    viewModel: UtilitiesViewModel = hiltViewModel(),
) {
    // collectAsStateWithLifecycle: ngừng nhận khi activity stop, không rò rỉ và
    // không vẽ lại vô ích. Bản `collectAsState` không lifecycle-aware chỉ dành cho
    // thư viện multiplatform; app Android thì dùng bản này.
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    UtilitiesScreen(
        uiState = uiState,
        onAction = viewModel::onAction,
        modifier = modifier,
    )
}
