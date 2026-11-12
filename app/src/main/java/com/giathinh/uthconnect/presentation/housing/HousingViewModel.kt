package com.giathinh.uthconnect.presentation.housing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.giathinh.uthconnect.domain.model.HousingPriceRange
import com.giathinh.uthconnect.domain.usecase.GetHousingListingsUseCase
import com.giathinh.uthconnect.domain.usecase.ReportHousingListingUseCase
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
 * Event một lần của module, khai ngay trong file ViewModel theo
 * docs/CODING_STANDARDS.md mục 6 (không thêm file thứ sáu).
 */
sealed interface HousingEvent {
    data object ReportAccepted : HousingEvent
    data object ReportRejected : HousingEvent

    /** Tải lại nền thất bại khi feed đã có dữ liệu — báo riêng, không thay feed bằng màn lỗi. */
    data object RefreshFailed : HousingEvent

    /** Gọi nhanh chủ tin từ feed — Route mở trình quay số, Screen không chạm Context. */
    data class CallPhone(val phoneNumber: String) : HousingEvent
}

@HiltViewModel
class HousingViewModel @Inject constructor(
    private val getListings: GetHousingListingsUseCase,
    private val reportListing: ReportHousingListingUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HousingUiState())

    /** Nguồn sự thật duy nhất của màn hình; feed lọc là thuộc tính dẫn xuất trong [HousingUiState]. */
    val uiState: StateFlow<HousingUiState> = _uiState.asStateFlow()

    private val _events = Channel<HousingEvent>(Channel.BUFFERED)

    /** Event một lần, thu trong `HousingRoute` — docs/CODING_STANDARDS.md mục 6. */
    val events = _events.receiveAsFlow()

    private var loadJob: Job? = null
    private var reportJob: Job? = null

    init {
        loadListings()
    }

    fun onAction(action: HousingAction) {
        when (action) {
            is HousingAction.Search,
            is HousingAction.ToggleSaved,
            is HousingAction.CallHost,
            -> handleQuickAction(action)

            is HousingAction.SelectType -> _uiState.update { it.copy(selectedType = action.type) }

            is HousingAction.SelectCampus -> _uiState.update { it.copy(selectedCampus = action.campus) }

            is HousingAction.SelectGender -> _uiState.update { it.copy(genderPreference = action.gender) }

            is HousingAction.SelectPriceRange -> _uiState.update { it.copy(priceRange = action.range) }

            is HousingAction.SelectMaxRent -> _uiState.update { it.copy(maxRentVnd = action.monthlyRentVnd) }

            is HousingAction.SelectMaxDistance -> _uiState.update { it.copy(maxDistanceM = action.meters) }

            is HousingAction.SelectSort -> _uiState.update { it.copy(sort = action.sort) }

            HousingAction.ClearFilters -> _uiState.update {
                it.copy(
                    searchQuery = "",
                    selectedType = null,
                    selectedCampus = null,
                    priceRange = HousingPriceRange.ALL,
                    maxRentVnd = null,
                    maxDistanceM = null,
                    genderPreference = null,
                    sort = HousingSort.NEWEST,
                )
            }

            HousingAction.Retry,
            HousingAction.Refresh,
            -> loadListings(showLoading = action == HousingAction.Retry)

            HousingAction.OpenFilters -> _uiState.update { it.copy(isFilterDialogOpen = true) }

            HousingAction.CloseFilters -> _uiState.update { it.copy(isFilterDialogOpen = false) }

            is HousingAction.OpenReport,
            is HousingAction.SelectReportReason,
            HousingAction.CloseReport,
            HousingAction.ConfirmReport,
            -> handleReportAction(action)
        }
    }

    /** Gom nhóm action nhanh của feed (tìm kiếm, lưu tim, gọi) để [onAction] không vượt ngưỡng phức tạp. */
    private fun handleQuickAction(action: HousingAction) {
        when (action) {
            is HousingAction.Search -> _uiState.update { it.copy(searchQuery = action.query) }

            is HousingAction.ToggleSaved -> _uiState.update { state ->
                val saved = if (state.savedListingIds.contains(action.id)) {
                    state.savedListingIds - action.id
                } else {
                    state.savedListingIds + action.id
                }
                state.copy(savedListingIds = saved)
            }

            is HousingAction.CallHost -> callHost(action.listingId)

            // Các action còn lại đã xử ở onAction, không rơi vào đây.
            else -> Unit
        }
    }

    /** Chỉ phát event khi tin có số điện thoại — Route mở trình quay số, không lộ SĐT trên UI feed. */
    private fun callHost(listingId: String) {
        val phone = _uiState.value.listings.find { it.id == listingId }?.contactPhone
        if (!phone.isNullOrBlank()) {
            viewModelScope.launch { _events.send(HousingEvent.CallPhone(phone)) }
        }
    }

    /** Gom nhóm action của dialog báo tin để [onAction] không vượt ngưỡng phức tạp. */
    private fun handleReportAction(action: HousingAction) {
        when (action) {
            is HousingAction.OpenReport -> _uiState.update {
                it.copy(reportingListingId = action.listingId, reportingReason = null)
            }

            is HousingAction.SelectReportReason -> _uiState.update { state ->
                state.copy(reportingReason = action.reason)
            }

            HousingAction.CloseReport -> _uiState.update {
                it.copy(reportingListingId = null, reportingReason = null)
            }

            HousingAction.ConfirmReport -> reportSelectedListing()

            // Các action còn lại đã xử ở onAction, không rơi vào đây.
            else -> Unit
        }
    }

    /** `showLoading = false` khi nạp lại sau báo tin — feed không được nháy spinner giữa lúc đọc. */
    private fun loadListings(showLoading: Boolean = true) {
        // Hủy yêu cầu cũ để kết quả về chậm không ghi đè feed của lần tải mới hơn.
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = if (showLoading) true else it.isLoading, failure = null) }
            getListings().fold(
                onSuccess = { items ->
                    _uiState.update { it.copy(isLoading = false, listings = items, failure = null) }
                },
                onFailure = { onLoadFailed() },
            )
        }
    }

    /** Chỉ chặn màn hình khi chưa có dữ liệu; đã có feed thì giữ nguyên và báo lỗi tải lại riêng. */
    private suspend fun onLoadFailed() {
        if (_uiState.value.listings.isEmpty()) {
            _uiState.update { it.copy(isLoading = false, failure = HousingFailure.LoadFailed) }
        } else {
            _uiState.update { it.copy(isLoading = false) }
            _events.send(HousingEvent.RefreshFailed)
        }
    }

    private fun reportSelectedListing() {
        if (reportJob?.isActive == true) return
        val snapshot = _uiState.value
        val listingId = snapshot.reportingListingId
        // Lý do do người dùng chọn trong dialog; gửi bừa một lý do mặc định sẽ khiến
        // backend xử phạt nhầm chủ tin.
        val reason = snapshot.reportingReason
        if (listingId == null || reason == null) return
        reportJob = viewModelScope.launch {
            reportListing(listingId = listingId, reason = reason.name).fold(
                onSuccess = {
                    _uiState.update { it.copy(reportingListingId = null, reportingReason = null) }
                    // Tin có thể bị ẩn ngay vì vượt ngưỡng báo cáo (README 3.2), nên
                    // nạp lại từ nguồn thay vì tự biên tập danh sách đang giữ trong state.
                    loadListings(showLoading = false)
                    _events.send(HousingEvent.ReportAccepted)
                },
                onFailure = {
                    // Dialog giữ nguyên để người dùng bấm gửi lại; lỗi không chặn feed.
                    _events.send(HousingEvent.ReportRejected)
                },
            )
        }
    }
}
