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
    private val serverConfirmedReadIds = mutableSetOf<String>()
    private data class CompletedReadMutation(
        val ids: Set<String>,
        val succeeded: Boolean,
    )
    private val completedReadMutations = mutableListOf<CompletedReadMutation>()
    private data class FailedReadMutation(
        val ids: List<String>,
        val markAll: Boolean,
    )
    private val failedReadMutations = linkedMapOf<Long, FailedReadMutation>()
    private var nextReadMutationVersion = 0L
    private var inboxLoadGeneration = 0L
    private var inboxLoadJob: Job? = null
    private var detailLoadGeneration = 0L

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
                startReadMutation(ids, markAll = true)
            }

            is NotificationAction.ClickNotification -> {
                if (action.notificationId in pendingReadIds) {
                    viewModelScope.launch {
                        _events.emit(NotificationNavigationEvent.OpenDetail(action.notificationId))
                    }
                    return
                }
                val ids = listOf(action.notificationId)
                startReadMutation(ids, markAll = false)
                viewModelScope.launch {
                    _events.emit(NotificationNavigationEvent.OpenDetail(action.notificationId))
                }
            }

            is NotificationAction.Refresh -> {
                loadNotifications(isRefresh = true)
            }

            is NotificationAction.LoadDetail -> loadDetail(action.notificationId)

            NotificationAction.RetryReadMutations -> retryFailedReadMutations()
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
                            inboxErrorMessage = error.localizedMessage,
                        )
                    }
                }
                .collect { list ->
                    if (generation != inboxLoadGeneration) return@collect
                    val serverReadIds = list
                        .filter { item -> item.readAt != null }
                        .map { item -> item.id }
                        .toSet()
                    clearServerConfirmedReadFailures(serverReadIds)
                    _uiState.update {
                        val completedAfterStart = completedReadMutations.drop(completedMutationsAtStart)
                        val readIds = if (
                            requestReadVersion == nextReadMutationVersion && completedAfterStart.isEmpty()
                        ) {
                            serverReadIds + serverConfirmedReadIds
                        } else {
                            // Keep optimistic mutations created after this GET started;
                            // an older response must not erase a newer read action.
                            val latestMutationById = linkedMapOf<String, Boolean>()
                            completedAfterStart.forEach { mutation ->
                                mutation.ids.forEach { id ->
                                    latestMutationById[id] = latestMutationById[id] == true || mutation.succeeded
                                }
                            }
                            latestMutationById.entries.fold(
                                it.readNotificationIds + serverReadIds + serverConfirmedReadIds,
                            ) { ids, (id, succeeded) ->
                                val isConfirmedRead = id in serverReadIds || id in serverConfirmedReadIds
                                if (succeeded || isConfirmedRead) ids + id else ids - id
                            }
                        }
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            notifications = list,
                            readNotificationIds = readIds + pendingReadIds,
                            readMutationFailed = failedReadMutations.isNotEmpty(),
                            inboxErrorMessage = null,
                        )
                    }
                }
        }
    }

    private fun clearServerConfirmedReadFailures(serverReadIds: Set<String>) {
        serverConfirmedReadIds += serverReadIds
        failedReadMutations.entries.removeIf { (_, mutation) ->
            mutation.ids.isNotEmpty() && mutation.ids.all { it in serverConfirmedReadIds }
        }
    }

    private fun loadDetail(notificationId: String) {
        val generation = ++detailLoadGeneration
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
                    if (generation != detailLoadGeneration || _uiState.value.detailId != notificationId) return@fold
                    _uiState.update { it.copy(detail = detail, isDetailLoading = false) }
                    if (detail != null) {
                        if (detail.readAt != null) {
                            clearServerConfirmedReadFailures(setOf(notificationId))
                            _uiState.update {
                                it.copy(
                                    readNotificationIds = it.readNotificationIds + notificationId,
                                    readMutationFailed = failedReadMutations.isNotEmpty(),
                                )
                            }
                        } else {
                            val version = markReadOptimistically(listOf(notificationId))
                            viewModelScope.launch {
                                val result = readMutationMutex.withLock {
                                    notificationRepository.markInboxNotificationRead(notificationId)
                                }
                                finishReadMutation(
                                    listOf(notificationId),
                                    version,
                                    result,
                                    markAll = false,
                                )
                            }
                        }
                    }
                },
                onFailure = { error ->
                    if (generation != detailLoadGeneration || _uiState.value.detailId != notificationId) return@fold
                    _uiState.update { it.copy(isDetailLoading = false, errorMessage = error.localizedMessage) }
                },
            )
        }
    }

    private fun markReadOptimistically(ids: List<String>): Long {
        val version = ++nextReadMutationVersion
        val previouslyRead = serverConfirmedReadIds.intersect(ids.toSet())
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

    private fun startReadMutation(ids: List<String>, markAll: Boolean) {
        val version = markReadOptimistically(ids)
        viewModelScope.launch {
            val result = readMutationMutex.withLock {
                if (markAll) {
                    notificationRepository.markAllInboxNotificationsRead()
                } else {
                    notificationRepository.markInboxNotificationRead(ids.single())
                }
            }
            finishReadMutation(ids, version, result, markAll)
            if (markAll && result.isSuccess) loadNotifications(isRefresh = true)
        }
    }

    private fun retryFailedReadMutations() {
        val failed = failedReadMutations.values.toList()
        failedReadMutations.clear()
        _uiState.update { it.copy(readMutationFailed = false) }
        failed.forEach { mutation -> startReadMutation(mutation.ids, mutation.markAll) }
    }

    private fun finishReadMutation(
        ids: List<String>,
        version: Long,
        result: Result<Unit>,
        markAll: Boolean,
    ) {
        val succeeded = result.isSuccess
        val ownedIds = ids.filter { readMutationVersions[it] == version }
        if (succeeded) serverConfirmedReadIds += ids
        if (ownedIds.isEmpty() && ids.isNotEmpty()) return
        ownedIds.forEach { id ->
            pendingReadIds.remove(id)
            readMutationVersions.remove(id)
        }
        val previous = readMutationPreviousStates.remove(version).orEmpty().intersect(ownedIds.toSet())
        if (ownedIds.isNotEmpty()) completedReadMutations += CompletedReadMutation(ownedIds.toSet(), succeeded)
        if (!succeeded) {
            val unconfirmedIds = ids.filterNot { it in serverConfirmedReadIds }
            if (unconfirmedIds.isNotEmpty()) {
                failedReadMutations[version] = FailedReadMutation(unconfirmedIds, markAll)
            } else {
                failedReadMutations.remove(version)
            }
            _uiState.update { state ->
                val serverConfirmed = serverConfirmedReadIds.intersect(ownedIds.toSet())
                state.copy(
                    readNotificationIds = (state.readNotificationIds - ownedIds.toSet()) + previous + serverConfirmed,
                    readMutationFailed = failedReadMutations.isNotEmpty(),
                )
            }
        } else {
            failedReadMutations.entries.removeIf { (_, mutation) ->
                (markAll && mutation.markAll) ||
                    (mutation.ids.isNotEmpty() && mutation.ids.all { it in serverConfirmedReadIds })
            }
            _uiState.update { it.copy(readMutationFailed = failedReadMutations.isNotEmpty()) }
        }
    }
}
