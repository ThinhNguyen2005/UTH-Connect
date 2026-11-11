package com.giathinh.uthconnect.presentation.housing.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.giathinh.uthconnect.domain.usecase.AccessStudentSessionUseCase
import com.giathinh.uthconnect.domain.usecase.DeleteHousingListingUseCase
import com.giathinh.uthconnect.domain.usecase.GetHousingListingDetailUseCase
import com.giathinh.uthconnect.domain.usecase.MarkHousingListingFulfilledUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface HousingDetailEvent {
    data class CallPhone(val phoneNumber: String) : HousingDetailEvent
    data class OpenZalo(val url: String) : HousingDetailEvent
    data class OpenMap(val latitude: Double, val longitude: Double, val addressLabel: String) : HousingDetailEvent
    data object MarkFulfilledSuccess : HousingDetailEvent
    data object Deleted : HousingDetailEvent
    data object ActionFailed : HousingDetailEvent
}

@HiltViewModel
class HousingDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getListingDetail: GetHousingListingDetailUseCase,
    private val markFulfilled: MarkHousingListingFulfilledUseCase,
    private val deleteListing: DeleteHousingListingUseCase,
    private val sessions: AccessStudentSessionUseCase,
) : ViewModel() {

    private val listingId: String = checkNotNull(savedStateHandle["listingId"])

    private val _uiState = MutableStateFlow(HousingDetailUiState())
    val uiState: StateFlow<HousingDetailUiState> = _uiState.asStateFlow()

    private val _events = Channel<HousingDetailEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        loadDetail()
    }

    fun onAction(action: HousingDetailAction) {
        when (action) {
            HousingDetailAction.Retry -> loadDetail()

            HousingDetailAction.CallHost -> {
                val phone = _uiState.value.listing?.contactPhone
                if (!phone.isNullOrBlank()) {
                    viewModelScope.launch { _events.send(HousingDetailEvent.CallPhone(phone)) }
                }
            }

            HousingDetailAction.OpenZalo -> {
                val listing = _uiState.value.listing
                val url = if (listing != null) {
                    if (!listing.contactZaloUrl.isNullOrBlank()) {
                        listing.contactZaloUrl
                    } else if (!listing.contactPhone.isNullOrBlank()) {
                        "https://zalo.me/${listing.contactPhone}"
                    } else {
                        null
                    }
                } else {
                    null
                }
                if (!url.isNullOrBlank()) {
                    viewModelScope.launch { _events.send(HousingDetailEvent.OpenZalo(url)) }
                }
            }

            HousingDetailAction.OpenMap -> openMap()

            HousingDetailAction.MarkFulfilled -> {
                viewModelScope.launch {
                    _uiState.update { it.copy(isActionLoading = true) }
                    val result = markFulfilled(listingId)
                    if (result.isSuccess) {
                        _events.send(HousingDetailEvent.MarkFulfilledSuccess)
                        loadDetail()
                    } else {
                        _events.send(HousingDetailEvent.ActionFailed)
                        _uiState.update { it.copy(isActionLoading = false) }
                    }
                }
            }

            HousingDetailAction.Delete -> {
                viewModelScope.launch {
                    _uiState.update { it.copy(isActionLoading = true) }
                    val result = deleteListing(listingId)
                    if (result.isSuccess) {
                        _events.send(HousingDetailEvent.Deleted)
                    } else {
                        _events.send(HousingDetailEvent.ActionFailed)
                        _uiState.update { it.copy(isActionLoading = false) }
                    }
                }
            }
        }
    }

    /** Phát tọa độ + địa chỉ để Route dựng deep-link Chỉ đường; Screen không chạm Context. */
    private fun openMap() {
        val listing = _uiState.value.listing ?: return
        viewModelScope.launch {
            _events.send(HousingDetailEvent.OpenMap(listing.latitude, listing.longitude, listing.addressLabel))
        }
    }

    private fun loadDetail() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, failure = null) }
            val result = getListingDetail(listingId)
            result.onSuccess { listing ->
                val profile = sessions.observe().value.profile
                val isOwner = profile != null && listing.hostStudentId.isNotBlank() &&
                    listing.hostStudentId == profile.studentId
                _uiState.update { it.copy(isLoading = false, listing = listing, isOwner = isOwner, failure = null) }
            }.onFailure { error ->
                // Repository trả NoSuchElementException khi id không tồn tại → trạng thái "không tìm thấy"
                // khác với lỗi mạng để Screen chọn đúng thông điệp + lối thoát (Quay lại vs Thử lại).
                val failure = if (error is NoSuchElementException) {
                    HousingDetailFailure.NotFound
                } else {
                    HousingDetailFailure.LoadFailed
                }
                _uiState.update { it.copy(isLoading = false, failure = failure) }
            }
        }
    }
}
