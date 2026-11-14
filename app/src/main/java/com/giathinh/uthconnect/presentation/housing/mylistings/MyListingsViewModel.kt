package com.giathinh.uthconnect.presentation.housing.mylistings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.giathinh.uthconnect.domain.usecase.GetMyHousingListingsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Event một lần của màn, khai ngay trong file ViewModel theo
 * docs/CODING_STANDARDS.md mục 6 (không thêm file thứ sáu).
 */
sealed interface MyListingsEvent {
    /** Mở chi tiết tin — Route điều hướng, Screen không giữ NavController. */
    data class OpenListing(val listingId: String) : MyListingsEvent

    /** Tải lại nền thất bại khi đã có dữ liệu — báo riêng, không thay danh sách bằng màn lỗi. */
    data object RefreshFailed : MyListingsEvent
}

@HiltViewModel
class MyListingsViewModel @Inject constructor(
    private val getMyListings: GetMyHousingListingsUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MyListingsUiState())
    val uiState: StateFlow<MyListingsUiState> = _uiState.asStateFlow()

    private val _events = Channel<MyListingsEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var loadJob: Job? = null

    init {
        load()
    }

    fun onAction(action: MyListingsAction) {
        when (action) {
            MyListingsAction.Load -> load(showLoading = true)

            MyListingsAction.Refresh -> load(showLoading = false)

            is MyListingsAction.OpenListing ->
                viewModelScope.launch { _events.send(MyListingsEvent.OpenListing(action.listingId)) }
        }
    }

    /** `showLoading = false` khi nạp lại nền — danh sách không nháy spinner giữa lúc đọc. */
    private fun load(showLoading: Boolean = true) {
        // Hủy yêu cầu cũ để kết quả về chậm không ghi đè lần tải mới hơn.
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = if (showLoading) true else it.isLoading, failure = null) }
            getMyListings().fold(
                onSuccess = { items -> _uiState.update { it.copy(isLoading = false, listings = items) } },
                onFailure = { onLoadFailed() },
            )
        }
    }

    /** Chỉ chặn màn khi chưa có dữ liệu; đã có danh sách thì giữ nguyên và báo lỗi tải lại riêng. */
    private suspend fun onLoadFailed() {
        if (_uiState.value.listings.isEmpty()) {
            _uiState.update { it.copy(isLoading = false, failure = MyListingsFailure.LoadFailed) }
        } else {
            _uiState.update { it.copy(isLoading = false) }
            _events.send(MyListingsEvent.RefreshFailed)
        }
    }
}
