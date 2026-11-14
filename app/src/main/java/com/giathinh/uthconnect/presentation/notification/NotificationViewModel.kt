package com.giathinh.uthconnect.presentation.notification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.giathinh.uthconnect.domain.repository.NotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface NotificationNavigationEvent {
    data object Back : NotificationNavigationEvent
    data object OpenSettings : NotificationNavigationEvent
    data object OpenProfile : NotificationNavigationEvent
    data class OpenDetail(val notificationId: String) : NotificationNavigationEvent
}

@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationUiState(isLoading = true))
    val uiState: StateFlow<NotificationUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<NotificationNavigationEvent>()
    val events: SharedFlow<NotificationNavigationEvent> = _events.asSharedFlow()

    init {
        loadNotifications()
    }

    fun onAction(action: NotificationAction) {
        when (action) {
            is NotificationAction.Back -> {
                viewModelScope.launch { _events.emit(NotificationNavigationEvent.Back) }
            }
            is NotificationAction.OpenSettings -> {
                viewModelScope.launch { _events.emit(NotificationNavigationEvent.OpenSettings) }
            }
            is NotificationAction.OpenProfile -> {
                viewModelScope.launch { _events.emit(NotificationNavigationEvent.OpenProfile) }
            }
            is NotificationAction.SelectCategory -> {
                _uiState.update { it.copy(selectedCategory = action.category) }
            }
            is NotificationAction.MarkAllAsRead -> {
                val previousIds = _uiState.value.readNotificationIds
                _uiState.update { state ->
                    state.copy(readNotificationIds = state.notifications.map { it.id }.toSet())
                }
                viewModelScope.launch {
                    val result = notificationRepository.markAllInboxNotificationsRead()
                    if (result.isFailure) {
                        // Khôi phục trạng thái optimistic nếu ghi thất bại
                        _uiState.update { it.copy(readNotificationIds = previousIds) }
                    }
                }
            }
            is NotificationAction.ClickNotification -> {
                val previousIds = _uiState.value.readNotificationIds
                _uiState.update { state ->
                    state.copy(readNotificationIds = state.readNotificationIds + action.notificationId)
                }
                viewModelScope.launch {
                    val result = notificationRepository.markInboxNotificationRead(action.notificationId)
                    if (result.isFailure) {
                        // Khôi phục trạng thái optimistic nếu ghi thất bại
                        _uiState.update { it.copy(readNotificationIds = previousIds) }
                    }
                }
                viewModelScope.launch {
                    _events.emit(NotificationNavigationEvent.OpenDetail(action.notificationId))
                }
            }
            is NotificationAction.Refresh -> {
                loadNotifications(isRefresh = true)
            }
            is NotificationAction.LoadDetail -> loadDetail(action.notificationId)
        }
    }

    private fun loadNotifications(isRefresh: Boolean = false) {
        _uiState.update { it.copy(isLoading = !isRefresh, isRefreshing = isRefresh) }
        viewModelScope.launch {
            notificationRepository.getInbox()
                .catch { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            errorMessage = error.localizedMessage,
                        )
                    }
                }
                .collect { list ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            notifications = list,
                            readNotificationIds = list.filter { item -> item.readAt != null }.map { item -> item.id }.toSet(),
                            errorMessage = null,
                        )
                    }
                }
        }
    }

    private fun loadDetail(notificationId: String) {
        _uiState.update { it.copy(detailId = notificationId, detail = null, isDetailLoading = true, errorMessage = null) }
        viewModelScope.launch {
            notificationRepository.getInboxNotification(notificationId).fold(
                onSuccess = { detail ->
                    _uiState.update { it.copy(detail = detail, isDetailLoading = false) }
                    // Đánh dấu đã đọc giống luồng ClickNotification
                    val previousIds = _uiState.value.readNotificationIds
                    _uiState.update { state ->
                        state.copy(readNotificationIds = state.readNotificationIds + notificationId)
                    }
                    val result = notificationRepository.markInboxNotificationRead(notificationId)
                    if (result.isFailure && notificationId !in previousIds) {
                        // Chỉ gỡ ID hiện tại nếu notification chưa được đọc từ trước.
                        _uiState.update { currentState ->
                            currentState.copy(
                                readNotificationIds = currentState.readNotificationIds - notificationId
                            )
                        }
                    }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(isDetailLoading = false, errorMessage = error.localizedMessage) }
                },
            )
        }
    }
}
