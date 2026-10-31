package com.giathinh.uthconnect.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.giathinh.uthconnect.domain.usecase.GetHomeFeedUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Giữ toàn bộ trạng thái của tab Trang chủ theo mô hình UDF:
 *
 * ```
 * UiState ──▶ Screen ──▶ Action ──▶ ViewModel ──▶ UseCase ──▶ Repository
 * ```
 *
 * - Không `Context`/`Modifier`/`@StringRes`; `_uiState` private, chỉ expose `StateFlow`.
 * - Không giữ danh sách gốc ở biến ngoài `UiState`: bộ lọc là `selectedCategory`, danh sách
 *   hiển thị suy ra bằng `visibleAnnouncements` → chỉ một nguồn sự thật.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getHomeFeed: GetHomeFeedUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    /** Điểm vào duy nhất: Screen chỉ phát action, không tự nạp lại. */
    fun onAction(action: HomeAction) {
        when (action) {
            HomeAction.Retry -> load()

            is HomeAction.SelectCategory -> _uiState.update { it.copy(selectedCategory = action.category) }

            // Ba action dưới đây để trống có chủ đích: màn hình đích (thông báo, bộ lọc,
            // lưu trữ) chưa có graph con. Giữ action để khi branch của module Sự kiện thêm
            // `HomeNavigation.kt` chỉ phải thay phần thân.
            HomeAction.OpenNotifications -> Unit
            HomeAction.OpenFilter -> Unit
            HomeAction.OpenArchive -> Unit

            // Chi tiết thông báo đã có graph riêng: `HomeRoute` chặn action này để điều hướng,
            // ViewModel không nhận. Nhánh vẫn phải có để `when` đủ.
            is HomeAction.OpenAnnouncement -> Unit
        }
    }

    private fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, failure = null) }
            getHomeFeed().fold(
                onSuccess = { feed ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            banners = feed.banners,
                            announcements = feed.latest,
                            failure = null,
                        )
                    }
                },
                onFailure = {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            banners = emptyList(),
                            announcements = emptyList(),
                            failure = HomeFailure.LoadFailed,
                        )
                    }
                },
            )
        }
    }
}
