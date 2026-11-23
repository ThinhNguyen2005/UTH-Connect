package com.giathinh.uthconnect.presentation.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
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
    onOpenNotifications: () -> Unit = {},
    onOpenEvent: (String) -> Unit = {},
    onOpenAllEvents: () -> Unit = {},
    onOpenHousingDetail: (String) -> Unit = {},
    onOpenAllHousing: () -> Unit = {},
    onCreateHousingPost: () -> Unit = {},
    onOpenBookDetail: (String) -> Unit = {},
    onOpenAllBooks: () -> Unit = {},
    onOpenLostFoundDetail: (String) -> Unit = {},
    onOpenAllLostFound: () -> Unit = {},
    onCreateLostFoundPost: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    HomeScreen(
        uiState = uiState,
        onAction = { action ->
            when (action) {
                is HomeAction.OpenAnnouncement -> onOpenAnnouncement(action.announcementId)
                is HomeAction.OpenNotifications -> onOpenNotifications()
                is HomeAction.OpenEvent -> onOpenEvent(action.eventId)
                HomeAction.OpenAllEvents -> onOpenAllEvents()
                is HomeAction.OpenHousingDetail -> onOpenHousingDetail(action.listingId)
                HomeAction.OpenAllHousing -> onOpenAllHousing()
                HomeAction.CreateHousingPost -> onCreateHousingPost()
                is HomeAction.OpenBookDetail -> onOpenBookDetail(action.bookId)
                HomeAction.OpenAllBooks -> onOpenAllBooks()
                is HomeAction.OpenLostFoundDetail -> onOpenLostFoundDetail(action.itemId)
                HomeAction.OpenAllLostFound -> onOpenAllLostFound()
                HomeAction.CreateLostFoundPost -> onCreateLostFoundPost()
                else -> viewModel.onAction(action)
            }
        },
        modifier = modifier,
    )
}
