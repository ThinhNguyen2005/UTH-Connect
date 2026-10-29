package com.giathinh.uthconnect.presentation.utilities

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.giathinh.uthconnect.domain.usecase.GetUtilityModulesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Giữ toàn bộ trạng thái của tab Tiện ích theo mô hình UDF:
 *
 * ```
 * UiState ──▶ Screen ──▶ Action ──▶ ViewModel ──▶ UseCase ──▶ Repository
 *                  (một chiều, không đường tắt nào khác)
 * ```
 *
 * - Không import `android.*` ngoài `androidx.lifecycle`, không cầm `Context`,
 *   `Modifier` hay `@StringRes` (`README.md` mục 5.2).
 * - `_uiState` private, chỉ expose `StateFlow` đọc được.
 * - Mọi cập nhật qua `update { it.copy(...) }` để không có race khi nhiều nguồn
 *   ghi cùng lúc.
 *
 * `@HiltViewModel` + `@Inject constructor`: Hilt dựng ViewModel qua
 * `ViewModelProvider` chứ không gọi constructor trực tiếp, nên unit test vẫn new
 * bình thường bằng fake (`UtilitiesViewModelTest`) — không cần rule Hilt.
 */
@HiltViewModel
class UtilitiesViewModel @Inject constructor(
    private val getUtilityModules: GetUtilityModulesUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(UtilitiesUiState())
    val uiState: StateFlow<UtilitiesUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    /** Điểm nạp duy nhất: Screen không tự gọi, chỉ phát [UtilitiesAction.Retry]. */
    fun onAction(action: UtilitiesAction) {
        when (action) {
            UtilitiesAction.Retry -> load()
            is UtilitiesAction.OpenModule -> {
                // Để trống có chủ đích — module chưa có graph con. Giữ action ở đây
                // để branch của module chỉ cần thay phần thân, không đổi chữ ký Screen.
            }
        }
    }

    private fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, failure = null) }
            getUtilityModules().fold(
                onSuccess = { modules ->
                    _uiState.update { it.copy(isLoading = false, modules = modules, failure = null) }
                },
                onFailure = {
                    _uiState.update {
                        it.copy(isLoading = false, modules = emptyList(), failure = UtilitiesFailure.LoadFailed)
                    }
                },
            )
        }
    }
}
