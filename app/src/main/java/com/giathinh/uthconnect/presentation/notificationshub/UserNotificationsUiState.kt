package com.giathinh.uthconnect.presentation.notificationshub

import com.giathinh.uthconnect.domain.model.NotificationItem

data class UserNotificationsUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val notifications: List<NotificationItem> = emptyList(),
    val errorMessage: String? = null,
)

sealed interface UserNotificationsAction {
    data object Refresh : UserNotificationsAction
    data class Open(val notification: NotificationItem) : UserNotificationsAction
}

sealed interface UserNotificationsEvent {
    data object OpenHousing : UserNotificationsEvent
    data object OpenEvents : UserNotificationsEvent
    data object OpenIncidents : UserNotificationsEvent
}
