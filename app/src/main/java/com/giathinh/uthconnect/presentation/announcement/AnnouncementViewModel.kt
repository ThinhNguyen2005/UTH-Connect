package com.giathinh.uthconnect.presentation.announcement

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.giathinh.uthconnect.domain.usecase.GetAnnouncementDetailUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Giữ trạng thái màn hình Chi tiết thông báo, theo đúng mô hình UDF như tab Trang chủ:
 *
 * ```
 * UiState ──▶ Screen ──▶ Action ──▶ ViewModel ──▶ UseCase ──▶ Repository
 * ```
 *
 * `announcementId` đọc từ [SavedStateHandle] — Navigation ghi tham số của route type-safe dưới
 * đúng tên thuộc tính (`announcementId`), nên ViewModel không cần biết gì về Navigation
 * (`docs/CODING_STANDARDS.md` mục 3.1) và test được bằng `SavedStateHandle` thuần.
 *
 * Không `Context`/`Modifier`/`@StringRes`; `_uiState` private, chỉ expose `StateFlow`.
 */
@HiltViewModel
class AnnouncementViewModel @Inject constructor(
    private val getAnnouncementDetail: GetAnnouncementDetailUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val announcementId: String = checkNotNull(
        savedStateHandle.get<String>(AnnouncementDetailDestination::announcementId.name),
    ) {
        "Route AnnouncementDetailDestination thiếu tham số announcementId"
    }

    private val _uiState = MutableStateFlow(AnnouncementUiState())
    val uiState: StateFlow<AnnouncementUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    /** Điểm vào duy nhất: Screen chỉ phát action, không tự nạp lại. */
    fun onAction(action: AnnouncementAction) {
        when (action) {
            AnnouncementAction.Retry -> load()
        }
    }

    private fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, failure = null) }
            getAnnouncementDetail(announcementId).fold(
                onSuccess = { detail ->
                    // `null` là "không tìm thấy": hạ `detail` xuống rỗng để UiState báo isMissing.
                    _uiState.update {
                        it.copy(isLoading = false, detail = detail, failure = null)
                    }
                },
                onFailure = {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            detail = null,
                            failure = AnnouncementFailure.LoadFailed,
                        )
                    }
                },
            )
        }
    }
}
