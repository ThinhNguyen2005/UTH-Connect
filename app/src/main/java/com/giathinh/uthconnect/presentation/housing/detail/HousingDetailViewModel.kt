package com.giathinh.uthconnect.presentation.housing.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.giathinh.uthconnect.domain.model.HousingContactStatus
import com.giathinh.uthconnect.domain.model.ServerErrorCode
import com.giathinh.uthconnect.domain.repository.HousingRepository
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
    data object ContactRequestSent : HousingDetailEvent
    data class ContactActionFailed(val code: ServerErrorCode) : HousingDetailEvent
    data object ActionFailed : HousingDetailEvent
}

@HiltViewModel
class HousingDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getListingDetail: GetHousingListingDetailUseCase,
    private val markFulfilled: MarkHousingListingFulfilledUseCase,
    private val housing: HousingRepository,
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
            HousingDetailAction.RequestContact -> requestContact()
            HousingDetailAction.CancelContact -> cancelContact()
            is HousingDetailAction.RespondContact -> respondContact(action.requestId, action.accept)
            HousingDetailAction.CallHost -> callHost()
            HousingDetailAction.OpenZalo -> openZalo()
            HousingDetailAction.OpenMap -> openMap()
            HousingDetailAction.MarkFulfilled -> markListingFulfilled()
        }
    }

    /** Chỉ phát SĐT khi đã mở khóa; trước ACCEPTED state không có privateDetails. */
    private fun callHost() {
        val phone = _uiState.value.privateDetails?.contactPhone
        if (!phone.isNullOrBlank()) {
            viewModelScope.launch { _events.send(HousingDetailEvent.CallPhone(phone)) }
        }
    }

    private fun openZalo() {
        val details = _uiState.value.privateDetails ?: return
        val url = details.contactZaloUrl.ifBlank { "https://zalo.me/${details.contactPhone}" }
        if (url.isNotBlank()) {
            viewModelScope.launch { _events.send(HousingDetailEvent.OpenZalo(url)) }
        }
    }

    /** Phát tọa độ + nhãn để Route dựng deep-link; trước mở khóa dùng tọa độ xấp xỉ. */
    private fun openMap() {
        val listing = _uiState.value.listing ?: return
        val details = _uiState.value.privateDetails
        val latitude = details?.latitude ?: listing.approxLatitude
        val longitude = details?.longitude ?: listing.approxLongitude
        val label = details?.exactAddress?.takeIf { it.isNotBlank() } ?: listing.addressLabel
        viewModelScope.launch {
            _events.send(HousingDetailEvent.OpenMap(latitude, longitude, label))
        }
    }

    private fun requestContact() {
        viewModelScope.launch {
            _uiState.update { it.copy(isActionLoading = true) }
            housing.requestContact(listingId, message = "").fold(
                onSuccess = {
                    _events.send(HousingDetailEvent.ContactRequestSent)
                    loadContactGate()
                },
                onFailure = { error ->
                    _events.send(HousingDetailEvent.ContactActionFailed(ServerErrorCode.of(error)))
                    _uiState.update { it.copy(isActionLoading = false) }
                },
            )
        }
    }

    private fun cancelContact() {
        val requestId = _uiState.value.myRequest?.id ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isActionLoading = true) }
            housing.cancelContact(requestId).fold(
                onSuccess = { loadContactGate() },
                onFailure = { error ->
                    _events.send(HousingDetailEvent.ContactActionFailed(ServerErrorCode.of(error)))
                    _uiState.update { it.copy(isActionLoading = false) }
                },
            )
        }
    }

    private fun respondContact(requestId: String, accept: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(isActionLoading = true) }
            housing.respondContact(requestId, accept).fold(
                onSuccess = { loadContactGate() },
                onFailure = { error ->
                    _events.send(HousingDetailEvent.ContactActionFailed(ServerErrorCode.of(error)))
                    _uiState.update { it.copy(isActionLoading = false) }
                },
            )
        }
    }

    private fun markListingFulfilled() {
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

    private fun loadDetail() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, failure = null) }
            val result = getListingDetail(listingId)
            result.onSuccess { listing ->
                _uiState.update { it.copy(isLoading = false, listing = listing, failure = null) }
                loadContactGate()
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

    /** Chủ tin thấy danh sách request; người hỏi thấy request của mình; ACCEPTED → mở khóa PII. */
    private suspend fun loadContactGate() {
        val isOwner = _uiState.value.isOwner
        if (isOwner) {
            housing.getContactRequests(listingId).onSuccess { requests ->
                _uiState.update { it.copy(contactRequests = requests, isActionLoading = false) }
            }.onFailure {
                _uiState.update { it.copy(isActionLoading = false) }
            }
        } else {
            housing.getMyContactRequest(listingId).onSuccess { request ->
                _uiState.update { it.copy(myRequest = request, isActionLoading = false) }
            }.onFailure {
                _uiState.update { it.copy(isActionLoading = false) }
            }
        }
        val accepted = isOwner || _uiState.value.myRequest?.status == HousingContactStatus.ACCEPTED
        if (accepted) {
            housing.revealPrivateDetails(listingId).onSuccess { details ->
                _uiState.update { it.copy(privateDetails = details) }
            }
            // Lỗi mở khóa không chặn xem tin công khai; UI giữ trạng thái chưa mở khóa.
        } else {
            _uiState.update { it.copy(privateDetails = null) }
        }
    }
}
