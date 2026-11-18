package com.giathinh.uthconnect.presentation.notificationshub

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.giathinh.uthconnect.domain.model.NotificationItem
import com.giathinh.uthconnect.domain.model.UserNotificationType
import com.giathinh.uthconnect.domain.repository.UserNotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
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
    private var refreshGeneration = 0L
    private val pendingReadIds = mutableSetOf<String>()
    private val confirmedReadIds = mutableSetOf<String>()
    private val readMutationGenerationById = mutableMapOf<String, Long>()
    private var nextReadMutationGeneration = 0L

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
        val generation = ++refreshGeneration
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
                    if (error is CancellationException) throw error
                    if (generation == refreshGeneration) {
                        mutableState.update {
                            it.copy(
                                isLoading = false,
                                isRefreshing = false,
                                errorMessage = error.localizedMessage
                                    ?: "Không thể tải thông báo. Vui lòng thử lại.",
                            )
                        }
                    }
                }
                .collect { serverNotifications ->
                    if (generation != refreshGeneration) return@collect
                    confirmedReadIds += serverNotifications.filter { it.isRead }.map { it.id }
                    val merged = serverNotifications.map { item ->
                        if (item.id in confirmedReadIds || item.id in pendingReadIds) {
                            item.copy(isRead = true)
                        } else {
                            item
                        }
                    }
                    mutableState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            notifications = merged,
                            errorMessage = null,
                        )
                    }
                }
            if (generation == refreshGeneration) repository.refreshUnreadCount()
        }
    }

    private fun openNotification(notification: NotificationItem) {
        val latest = mutableState.value.notifications.firstOrNull { it.id == notification.id } ?: notification
        if (!latest.isRead && latest.recipientId != null && pendingReadIds.add(latest.id)) {
            val readGeneration = ++nextReadMutationGeneration
            readMutationGenerationById[latest.id] = readGeneration
            mutableState.update { state ->
                state.copy(
                    notifications = state.notifications.map { item ->
                        if (item.id == latest.id) item.copy(isRead = true) else item
                    },
                )
            }
            viewModelScope.launch {
                val result = repository.markAsRead(latest.id)
                pendingReadIds.remove(latest.id)
                if (result.getOrNull() == true) {
                    confirmedReadIds += latest.id
                } else {
                    val error = result.exceptionOrNull()
                    val message = error?.localizedMessage
                        ?: error?.let { "Không thể cập nhật trạng thái thông báo. Vui lòng thử lại." }
                    mutableState.update { state ->
                        state.copy(
                            notifications = state.notifications.map { item ->
                                if (item.id == latest.id) {
                                    item.copy(isRead = latest.id in confirmedReadIds)
                                } else {
                                    item
                                }
                            },
                            errorMessage = message ?: state.errorMessage,
                        )
                    }
                    reconcileNotification(latest.id, readGeneration)
                }
                repository.refreshUnreadCount()
            }
        }
        viewModelScope.launch {
            when (latest.type) {
                UserNotificationType.HOUSING_APPROVED,
                UserNotificationType.HOUSING_REJECTED,
                -> eventChannel.send(UserNotificationsEvent.OpenHousing)

                UserNotificationType.EVENT_REMINDER -> eventChannel.send(UserNotificationsEvent.OpenEvents)

                UserNotificationType.SYSTEM -> Unit
            }
        }
    }

    private suspend fun reconcileNotification(notificationId: String, readGeneration: Long) {
        val serverItem = try {
            repository.getNotifications().first().firstOrNull { it.id == notificationId }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            null
        } ?: return
        if (readMutationGenerationById[notificationId] != readGeneration) return
        if (serverItem.isRead) confirmedReadIds += notificationId
        mutableState.update { state ->
            state.copy(
                notifications = state.notifications.map { item ->
                    if (item.id == notificationId) {
                        item.copy(isRead = serverItem.isRead || notificationId in confirmedReadIds)
                    } else {
                        item
                    }
                },
                errorMessage = if (serverItem.isRead) null else state.errorMessage,
            )
        }
    }
}

@HiltViewModel
class NotificationBadgeViewModel @Inject constructor(
    private val repository: UserNotificationRepository,
) : ViewModel() {
    val unreadCount = repository.unreadCount
    val currentUserId: String? get() = repository.currentUserId
    private var observedUserId = currentUserId

    fun onSignedInUserChanged() {
        val latestUserId = currentUserId
        if (latestUserId != observedUserId) {
            repository.clearUnreadCount()
            observedUserId = latestUserId
        }
        refresh()
    }

    fun refresh() {
        viewModelScope.launch { repository.refreshUnreadCount() }
    }

    override fun onCleared() {
        repository.clearUnreadCount()
        super.onCleared()
    }
}
