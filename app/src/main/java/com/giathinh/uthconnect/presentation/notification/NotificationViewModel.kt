package com.giathinh.uthconnect.presentation.notification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.giathinh.uthconnect.domain.repository.NotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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

    private val readMutationMutex = Mutex()
    private val pendingReadIds = mutableSetOf<String>()
    private val readMutationVersions = mutableMapOf<String, Long>()
    private val readMutationPreviousStates = mutableMapOf<Long, Set<String>>()
    private data class CompletedReadMutation(
        val ids: Set<String>,
        val succeeded: Boolean,
    )
    private val completedReadMutations = mutableListOf<CompletedReadMutation>()
    private var nextReadMutationVersion = 0L
    private var inboxLoadGeneration = 0L
    private var inboxLoadJob: Job? = null

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
                val currentState = _uiState.value
                val ids = currentState.notifications
                    .map { it.id }
                    .filterNot { it in currentState.readNotificationIds }
                val version = markReadOptimistically(ids)
                viewModelScope.launch {
                    val result = readMutationMutex.withLock {
                        notificationRepository.markAllInboxNotificationsRead()
                    }
                    finishReadMutation(ids, version, result.isSuccess)
                    if (result.isSuccess) loadNotifications(isRefresh = true)
                }
            }

            is NotificationAction.ClickNotification -> {
                val ids = listOf(action.notificationId)
                val version = markReadOptimistically(ids)
                viewModelScope.launch {
                    val result = readMutationMutex.withLock {
                        notificationRepository.markInboxNotificationRead(action.notificationId)
                    }
                    finishReadMutation(ids, version, result.isSuccess)
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
        val generation = ++inboxLoadGeneration
        inboxLoadJob?.cancel()
        val requestReadVersion = nextReadMutationVersion
        val completedMutationsAtStart = completedReadMutations.size
        _uiState.update { it.copy(isLoading = !isRefresh, isRefreshing = isRefresh) }
        inboxLoadJob = viewModelScope.launch {
            notificationRepository.getInbox()
                .catch { error ->
                    if (generation != inboxLoadGeneration) return@catch
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            errorMessage = error.localizedMessage,
                        )
                    }
                }
                .collect { list ->
                    if (generation != inboxLoadGeneration) return@collect
                    val serverReadIds = list
                        .filter { item -> item.readAt != null }
                        .map { item -> item.id }
                        .toSet()
                    _uiState.update {
                        val completedAfterStart = completedReadMutations.drop(completedMutationsAtStart)
                        val readIds = if (
                            requestReadVersion == nextReadMutationVersion && completedAfterStart.isEmpty()
                        ) {
                            serverReadIds
                        } else {
                            // Keep optimistic mutations created after this GET started;
                            // an older response must not erase a newer read action.
                            val latestMutationById = linkedMapOf<String, Boolean>()
                            completedAfterStart.forEach { mutation ->
                                mutation.ids.forEach { id -> latestMutationById[id] = mutation.succeeded }
                            }
                            latestMutationById.entries.fold(
                                it.readNotificationIds + serverReadIds,
                            ) { ids, (id, succeeded) ->
                                if (succeeded) ids + id else ids - id
                            }
                        }
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            notifications = list,
                            readNotificationIds = readIds + pendingReadIds,
                            errorMessage = null,
                        )
                    }
                }
        }
    }

    private fun loadDetail(notificationId: String) {
        _uiState.update {
            it.copy(
                detailId = notificationId,
                detail = null,
                isDetailLoading = true,
                errorMessage = null,
            )
        }
        viewModelScope.launch {
            notificationRepository.getInboxNotification(notificationId).fold(
                onSuccess = { detail ->
                    if (_uiState.value.detailId != notificationId) return@fold
                    _uiState.update { it.copy(detail = detail, isDetailLoading = false) }
                    if (detail != null) {
                        val version = markReadOptimistically(listOf(notificationId))
                        viewModelScope.launch {
                            val result = readMutationMutex.withLock {
                                notificationRepository.markInboxNotificationRead(notificationId)
                            }
                            finishReadMutation(listOf(notificationId), version, result.isSuccess)
                        }
                    }
                },
                onFailure = { error ->
                    if (_uiState.value.detailId != notificationId) return@fold
                    _uiState.update { it.copy(isDetailLoading = false, errorMessage = error.localizedMessage) }
                },
            )
        }
    }

    private fun markReadOptimistically(ids: List<String>): Long {
        val version = ++nextReadMutationVersion
        val previouslyRead = _uiState.value.readNotificationIds.intersect(ids.toSet())
        ids.forEach { id ->
            pendingReadIds += id
            readMutationVersions[id] = version
        }
        readMutationPreviousStates[version] = previouslyRead
        _uiState.update { state ->
            state.copy(readNotificationIds = state.readNotificationIds + ids)
        }
        return version
    }

    private fun finishReadMutation(ids: List<String>, version: Long, succeeded: Boolean) {
        val ownedIds = ids.filter { readMutationVersions[it] == version }
        if (ownedIds.isEmpty()) return
        ownedIds.forEach { id ->
            pendingReadIds.remove(id)
            readMutationVersions.remove(id)
        }
        val previous = readMutationPreviousStates.remove(version).orEmpty().intersect(ownedIds.toSet())
        completedReadMutations += CompletedReadMutation(ownedIds.toSet(), succeeded)
        if (!succeeded) {
            _uiState.update { state ->
                state.copy(readNotificationIds = (state.readNotificationIds - ownedIds.toSet()) + previous)
            }
        }
    }
}
