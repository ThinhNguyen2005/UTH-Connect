package com.giathinh.uthconnect.presentation.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Tầng nối của tab Trang chủ: chỗ **duy nhất** trong module lấy ViewModel ra khỏi Hilt.
 *
 * Tách khỏi [HomeScreen] để Screen stateless (preview được 4 trạng thái mà không cần
 * repository thật) và để ViewModel không bị truyền xuống composable con.
 *
 * Mở chi tiết thông báo đi qua [onOpenAnnouncement] thay vì `navigate` tại đây: màn hình Chi
 * tiết thuộc module `announcement`, nên tab Trang chủ không import module đó — chỉ chuyển id ra
 * ngoài cho graph tổng (`UthApp.kt`) nối route.
 *
 * `hiltViewModel()` scope theo `NavBackStackEntry` của `HomeDestination`, nên quay lại tab là
 * gặp lại đúng trạng thái và bộ lọc đang chọn.
 */
@Composable
fun HomeRoute(
    onOpenAnnouncement: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    HomeScreen(
        uiState = uiState,
        onAction = { action ->
            if (action is HomeAction.OpenAnnouncement) {
                onOpenAnnouncement(action.announcementId)
            } else {
                viewModel.onAction(action)
            }
        },
        modifier = modifier,
    )
}
