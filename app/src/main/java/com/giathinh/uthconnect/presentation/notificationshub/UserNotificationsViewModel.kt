package com.giathinh.uthconnect.presentation.notificationshub

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.giathinh.uthconnect.domain.model.NotificationItem
import com.giathinh.uthconnect.domain.model.UserNotificationType
import com.giathinh.uthconnect.domain.repository.UserNotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UserNotificationsViewModel @Inject constructor(
    private val repository: UserNotificationRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow(UserNotificationsUiState())
    val uiState = mutableState.asStateFlow()
    private val eventChannel = Channel<UserNotificationsEvent>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()
    private var refreshJob: Job? = null

    init {
        refresh()
    }

    fun onAction(action: UserNotificationsAction) {
        when (action) {
            UserNotificationsAction.Refresh -> refresh()
            is UserNotificationsAction.Open -> openNotification(action.notification)
        }
    }

    fun refresh() {
        refreshJob?.cancel()
        val refreshing = mutableState.value.notifications.isNotEmpty()
        mutableState.update {
            it.copy(
                isLoading = !refreshing,
                isRefreshing = refreshing,
                errorMessage = null,
            )
        }
        refreshJob = viewModelScope.launch {
            repository.getNotifications()
                .catch { error ->
                    mutableState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            errorMessage = error.localizedMessage,
                        )
                    }
                }
                .collect { notifications ->
                    mutableState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            notifications = notifications,
                            errorMessage = null,
                        )
                    }
                }
            repository.refreshUnreadCount()
        }
    }

    private fun openNotification(notification: NotificationItem) {
        if (!notification.isRead && notification.recipientId != null) {
            mutableState.update { state ->
                state.copy(
                    notifications = state.notifications.map { item ->
                        if (item.id == notification.id) item.copy(isRead = true) else item
                    },
                )
            }
            viewModelScope.launch {
                val result = repository.markAsRead(notification.id)
                if (result.isFailure) {
                    mutableState.update { state ->
                        state.copy(
                            notifications = state.notifications.map { item ->
                                if (item.id == notification.id) item.copy(isRead = false) else item
                            },
                            errorMessage = result.exceptionOrNull()?.localizedMessage,
                        )
                    }
                    repository.refreshUnreadCount()
                }
            }
        }
        viewModelScope.launch {
            when (notification.type) {
                UserNotificationType.HOUSING_APPROVED,
                UserNotificationType.HOUSING_REJECTED,
                -> eventChannel.send(UserNotificationsEvent.OpenHousing)

                UserNotificationType.EVENT_REMINDER -> eventChannel.send(UserNotificationsEvent.OpenEvents)

                UserNotificationType.SYSTEM -> Unit
            }
        }
    }
}

@HiltViewModel
class NotificationBadgeViewModel @Inject constructor(
    private val repository: UserNotificationRepository,
) : ViewModel() {
    val unreadCount = repository.unreadCount

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch { repository.refreshUnreadCount() }
    }
}
