package com.giathinh.uthconnect.presentation.notification

import com.giathinh.uthconnect.domain.model.NotificationCategory
import com.giathinh.uthconnect.domain.model.NotificationItem
import com.giathinh.uthconnect.domain.model.PostNotification
import com.giathinh.uthconnect.domain.model.UserNotificationType
import com.giathinh.uthconnect.domain.repository.NotificationRepository
import com.giathinh.uthconnect.domain.repository.UserNotificationRepository
import com.giathinh.uthconnect.testing.MainDispatcherRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class NotificationViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(StandardTestDispatcher())

    @Test
    fun `old inbox response keeps a read mutation that previously succeeded`() = runTest {
        val notification = notification("notification-1")
        val repository = FakeNotificationRepository(
            markResults = ArrayDeque(
                listOf(
                    Result.success(Unit),
                    Result.failure(IllegalStateException("temporary")),
                ),
            ),
        )
        val viewModel = createViewModel(repository)

        advanceUntilIdle()
        repository.emit(listOf(notification))
        advanceUntilIdle()
        viewModel.onAction(NotificationAction.Refresh)
        advanceUntilIdle()

        viewModel.onAction(NotificationAction.ClickNotification(notification.id))
        advanceUntilIdle()
        viewModel.onAction(NotificationAction.ClickNotification(notification.id))
        advanceUntilIdle()

        repository.emit(listOf(notification))
        advanceUntilIdle()

        assertTrue(notification.id in viewModel.uiState.value.readNotificationIds)
    }

    @Test
    fun `stale unread inbox keeps earlier success when a newer read mutation fails`() = runTest {
        val notification = notification("notification-confirmed-read")
        val firstPatch = CompletableDeferred<Unit>()
        val secondPatch = CompletableDeferred<Unit>()
        val repository = FakeNotificationRepository(
            markResults = ArrayDeque(
                listOf(
                    Result.success(Unit),
                    Result.failure(IllegalStateException("temporary")),
                ),
            ),
            markGates = ArrayDeque(listOf(firstPatch, secondPatch)),
            detail = notification,
        )
        val viewModel = createViewModel(repository)

        advanceUntilIdle()
        viewModel.onAction(NotificationAction.LoadDetail(notification.id))
        advanceUntilIdle()
        viewModel.onAction(NotificationAction.LoadDetail(notification.id))
        advanceUntilIdle()

        firstPatch.complete(Unit)
        advanceUntilIdle()
        secondPatch.complete(Unit)
        advanceUntilIdle()

        assertEquals(2, repository.markCallCount)

        repository.emit(listOf(notification))
        advanceUntilIdle()

        assertTrue(notification.id in viewModel.uiState.value.readNotificationIds)
    }

    @Test
    fun `two failed detail read mutations restore an unread notification`() = runTest {
        val notification = notification("notification-2")
        val firstPatch = CompletableDeferred<Unit>()
        val secondPatch = CompletableDeferred<Unit>()
        val repository = FakeNotificationRepository(
            markResults = ArrayDeque(
                listOf(
                    Result.failure(IllegalStateException("first")),
                    Result.failure(IllegalStateException("second")),
                ),
            ),
            markGates = ArrayDeque(listOf(firstPatch, secondPatch)),
            detail = notification,
        )
        val viewModel = createViewModel(repository)

        viewModel.onAction(NotificationAction.LoadDetail(notification.id))
        advanceUntilIdle()
        viewModel.onAction(NotificationAction.LoadDetail(notification.id))
        advanceUntilIdle()

        firstPatch.complete(Unit)
        advanceUntilIdle()
        secondPatch.complete(Unit)
        advanceUntilIdle()

        assertEquals(2, repository.markCallCount)

        assertTrue(notification.id !in viewModel.uiState.value.readNotificationIds)
    }

    @Test
    fun `mark all confirms an ID with an individual read mutation pending`() = runTest {
        val notification = notification("notification-mark-all-pending-read")
        val individualReadGate = CompletableDeferred<Unit>()
        val repository = FakeNotificationRepository(
            markResults = ArrayDeque(listOf(Result.failure(IllegalStateException("temporary")))),
            markAllResults = ArrayDeque(listOf(Result.success(Unit))),
            markGates = ArrayDeque(listOf(individualReadGate)),
        )
        val viewModel = createViewModel(repository)
        advanceUntilIdle()
        repository.emit(listOf(notification))
        advanceUntilIdle()

        viewModel.onAction(NotificationAction.ClickNotification(notification.id))
        advanceUntilIdle()
        viewModel.onAction(NotificationAction.MarkAllAsRead)
        advanceUntilIdle()
        individualReadGate.complete(Unit)
        advanceUntilIdle()

        assertTrue(notification.id in viewModel.uiState.value.readNotificationIds)
        assertTrue(!viewModel.uiState.value.readMutationFailed)
        assertTrue(repository.markCallCount == 1)
        assertTrue(repository.markAllCallCount == 1)
    }

    @Test
    fun `failed mark all read shows error and retries the mutation`() = runTest {
        val notification = notification("notification-mark-all")
        val repository = FakeNotificationRepository(
            markResults = ArrayDeque(),
            markAllResults = ArrayDeque(
                listOf(
                    Result.failure(IllegalStateException("temporary")),
                    Result.success(Unit),
                ),
            ),
        )
        val viewModel = createViewModel(repository)
        advanceUntilIdle()
        repository.emit(listOf(notification))
        advanceUntilIdle()

        viewModel.onAction(NotificationAction.MarkAllAsRead)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.readMutationFailed)
        assertTrue(notification.id !in viewModel.uiState.value.readNotificationIds)
        assertTrue(repository.markAllCallCount == 1)

        viewModel.onAction(NotificationAction.RetryReadMutations)
        advanceUntilIdle()

        assertTrue(!viewModel.uiState.value.readMutationFailed)
        assertTrue(notification.id in viewModel.uiState.value.readNotificationIds)
        assertTrue(repository.markAllCallCount == 2)
    }

    @Test
    fun `failed mark all before inbox loads is retained and retried`() = runTest {
        val repository = FakeNotificationRepository(
            markResults = ArrayDeque(),
            markAllResults = ArrayDeque(
                listOf(
                    Result.failure(IllegalStateException("temporary")),
                    Result.success(Unit),
                ),
            ),
        )
        val viewModel = createViewModel(repository)
        advanceUntilIdle()

        viewModel.onAction(NotificationAction.MarkAllAsRead)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.notifications.isEmpty())
        assertTrue(viewModel.uiState.value.readMutationFailed)
        assertTrue(repository.markAllCallCount == 1)

        viewModel.onAction(NotificationAction.RetryReadMutations)
        advanceUntilIdle()

        assertTrue(!viewModel.uiState.value.readMutationFailed)
        assertTrue(repository.markAllCallCount == 2)
    }

    @Test
    fun `superseded mark all failure is retained for retry`() = runTest {
        val notification = notification("notification-mark-all-superseded")
        val markAllGate = CompletableDeferred<Unit>()
        val repository = FakeNotificationRepository(
            markResults = ArrayDeque(listOf(Result.success(Unit))),
            markAllResults = ArrayDeque(
                listOf(
                    Result.failure(IllegalStateException("temporary")),
                    Result.success(Unit),
                ),
            ),
            markAllGates = ArrayDeque(listOf(markAllGate)),
            detail = notification,
        )
        val viewModel = createViewModel(repository)
        advanceUntilIdle()
        repository.emit(listOf(notification))
        advanceUntilIdle()

        viewModel.onAction(NotificationAction.MarkAllAsRead)
        advanceUntilIdle()
        viewModel.onAction(NotificationAction.LoadDetail(notification.id))
        advanceUntilIdle()
        markAllGate.complete(Unit)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.readMutationFailed)
        assertTrue(repository.markAllCallCount == 1)

        viewModel.onAction(NotificationAction.RetryReadMutations)
        advanceUntilIdle()

        assertTrue(!viewModel.uiState.value.readMutationFailed)
        assertTrue(repository.markAllCallCount == 2)
    }

    @Test
    fun `failed mark all stays retryable after individual reads are confirmed`() = runTest {
        val first = notification("notification-a")
        val second = notification("notification-b")
        val repository = FakeNotificationRepository(
            markResults = ArrayDeque(listOf(Result.success(Unit), Result.success(Unit))),
            markAllResults = ArrayDeque(
                listOf(
                    Result.failure(IllegalStateException("temporary")),
                    Result.success(Unit),
                ),
            ),
        )
        val viewModel = createViewModel(repository)
        advanceUntilIdle()
        repository.emit(listOf(first, second))
        advanceUntilIdle()

        viewModel.onAction(NotificationAction.MarkAllAsRead)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.readMutationFailed)

        viewModel.onAction(NotificationAction.ClickNotification(first.id))
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.readMutationFailed)

        viewModel.onAction(NotificationAction.ClickNotification(second.id))
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.readMutationFailed)
        assertTrue(first.id in viewModel.uiState.value.readNotificationIds)
        assertTrue(second.id in viewModel.uiState.value.readNotificationIds)

        viewModel.onAction(NotificationAction.RetryReadMutations)
        advanceUntilIdle()

        assertTrue(!viewModel.uiState.value.readMutationFailed)
        assertTrue(repository.markAllCallCount == 2)
    }

    @Test
    fun `server confirmed read clears failed mutation and its error`() = runTest {
        val notification = notification("notification-confirmed-by-refresh")
        val repository = FakeNotificationRepository(
            markResults = ArrayDeque(listOf(Result.failure(IllegalStateException("temporary")))),
        )
        val viewModel = createViewModel(repository)
        advanceUntilIdle()
        repository.emit(listOf(notification))
        advanceUntilIdle()

        viewModel.onAction(NotificationAction.ClickNotification(notification.id))
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.readMutationFailed)

        viewModel.onAction(NotificationAction.Refresh)
        advanceUntilIdle()
        repository.emit(listOf(notification.copy(readAt = 1_700_000_000_000L)))
        advanceUntilIdle()

        assertTrue(!viewModel.uiState.value.readMutationFailed)
        assertTrue(notification.id in viewModel.uiState.value.readNotificationIds)
    }

    @Test
    fun `read detail confirms state and skips a redundant patch`() = runTest {
        val notification = notification("notification-read-detail")
        val patchGate = CompletableDeferred<Unit>()
        val repository = FakeNotificationRepository(
            markResults = ArrayDeque(
                listOf(
                    Result.failure(IllegalStateException("temporary")),
                    Result.success(Unit),
                ),
            ),
            markGates = ArrayDeque(listOf(patchGate)),
            detail = notification.copy(readAt = 1_700_000_000_000L),
        )
        val viewModel = createViewModel(repository)
        advanceUntilIdle()

        viewModel.onAction(NotificationAction.ClickNotification(notification.id))
        advanceUntilIdle()
        viewModel.onAction(NotificationAction.LoadDetail(notification.id))
        advanceUntilIdle()

        assertTrue(notification.id in viewModel.uiState.value.readNotificationIds)
        assertTrue(!viewModel.uiState.value.readMutationFailed)
        assertTrue(repository.markCallCount == 1)

        patchGate.complete(Unit)
        advanceUntilIdle()

        assertTrue(notification.id in viewModel.uiState.value.readNotificationIds)
        assertTrue(!viewModel.uiState.value.readMutationFailed)
        assertTrue(repository.markCallCount == 1)
    }

    @Test
    fun `old unread inbox response keeps read state confirmed by detail`() = runTest {
        val notification = notification("notification-detail-read-confirmation")
        val repository = FakeNotificationRepository(
            markResults = ArrayDeque(),
            detail = notification.copy(readAt = 1_700_000_000_000L),
        )
        val viewModel = createViewModel(repository)
        advanceUntilIdle()

        viewModel.onAction(NotificationAction.Refresh)
        advanceUntilIdle()
        viewModel.onAction(NotificationAction.LoadDetail(notification.id))
        advanceUntilIdle()
        repository.emit(listOf(notification))
        advanceUntilIdle()

        assertTrue(notification.id in viewModel.uiState.value.readNotificationIds)
    }

    @Test
    fun `failed individual read shows error and retries the mutation`() = runTest {
        val notification = notification("notification-mark-one")
        val repository = FakeNotificationRepository(
            markResults = ArrayDeque(
                listOf(
                    Result.failure(IllegalStateException("temporary")),
                    Result.success(Unit),
                ),
            ),
        )
        val viewModel = createViewModel(repository)
        advanceUntilIdle()
        repository.emit(listOf(notification))
        advanceUntilIdle()

        viewModel.onAction(NotificationAction.ClickNotification(notification.id))
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.readMutationFailed)
        assertTrue(notification.id !in viewModel.uiState.value.readNotificationIds)
        assertTrue(repository.markCallCount == 1)

        viewModel.onAction(NotificationAction.RetryReadMutations)
        advanceUntilIdle()

        assertTrue(!viewModel.uiState.value.readMutationFailed)
        assertTrue(notification.id in viewModel.uiState.value.readNotificationIds)
        assertTrue(repository.markCallCount == 2)
    }

    @Test
    fun `stale successful read clears earlier failure before ownership return`() = runTest {
        val notification = notification("notification-stale-read-success")
        val initialFailureGate = CompletableDeferred<Unit>()
        val staleSuccessGate = CompletableDeferred<Unit>()
        val repository = FakeNotificationRepository(
            markResults = ArrayDeque(
                listOf(
                    Result.failure(IllegalStateException("initial failure")),
                    Result.success(Unit),
                    Result.failure(IllegalStateException("newer failure")),
                ),
            ),
            markGates = ArrayDeque(listOf(initialFailureGate, staleSuccessGate)),
            detail = notification,
        )
        val viewModel = createViewModel(repository)
        advanceUntilIdle()

        viewModel.onAction(NotificationAction.ClickNotification(notification.id))
        advanceUntilIdle()
        initialFailureGate.complete(Unit)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.readMutationFailed)

        viewModel.onAction(NotificationAction.LoadDetail(notification.id))
        advanceUntilIdle()
        viewModel.onAction(NotificationAction.LoadDetail(notification.id))
        advanceUntilIdle()

        staleSuccessGate.complete(Unit)
        advanceUntilIdle()

        assertTrue(notification.id in viewModel.uiState.value.readNotificationIds)
        assertTrue(!viewModel.uiState.value.readMutationFailed)
    }

    @Test
    fun `stale same detail failure does not end the latest loading request`() = runTest {
        val notification = notification("notification-detail-generation")
        val staleResult = CompletableDeferred<Result<PostNotification?>>()
        val latestResult = CompletableDeferred<Result<PostNotification?>>()
        val repository = FakeNotificationRepository(
            markResults = ArrayDeque(listOf(Result.success(Unit))),
            detailResults = ArrayDeque(listOf(staleResult, latestResult)),
        )
        val viewModel = createViewModel(repository)

        viewModel.onAction(NotificationAction.LoadDetail(notification.id))
        viewModel.onAction(NotificationAction.LoadDetail(notification.id))
        advanceUntilIdle()

        staleResult.complete(Result.failure(IllegalStateException("stale failure")))
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isDetailLoading)
        assertTrue(viewModel.uiState.value.errorMessage == null)

        latestResult.complete(Result.success(notification))
        advanceUntilIdle()

        assertTrue(!viewModel.uiState.value.isDetailLoading)
        assertTrue(viewModel.uiState.value.detail == notification)
    }

    @Test
    fun `successful inbox read refreshes badge without a route collector`() = runTest {
        val repository = FakeNotificationRepository(
            markResults = ArrayDeque(listOf(Result.success(Unit))),
        )
        val badgeRepository = FakeUserNotificationRepository()
        val viewModel = NotificationViewModel(repository, badgeRepository)

        viewModel.onAction(NotificationAction.ClickNotification("notification-1"))
        advanceUntilIdle()

        assertEquals(1, badgeRepository.scheduledRefreshCalls)
    }

    private fun createViewModel(repository: NotificationRepository): NotificationViewModel = NotificationViewModel(
        notificationRepository = repository,
        userNotificationRepository = FakeUserNotificationRepository(),
    )

    private fun notification(id: String) = PostNotification(
        id = id,
        title = "Thông báo",
        summary = "Tóm tắt",
        content = "Nội dung",
        category = NotificationCategory.GENERAL,
        author = "UTH Connect",
        publishedAt = 1_577_836_800_000L,
    )

    private class FakeNotificationRepository(
        private val markResults: ArrayDeque<Result<Unit>>,
        private val markAllResults: ArrayDeque<Result<Unit>> = ArrayDeque(listOf(Result.success(Unit))),
        private val markGates: ArrayDeque<CompletableDeferred<Unit>> = ArrayDeque(),
        private val markAllGates: ArrayDeque<CompletableDeferred<Unit>> = ArrayDeque(),
        private val detail: PostNotification? = null,
        private val detailResults: ArrayDeque<CompletableDeferred<Result<PostNotification?>>> = ArrayDeque(),
    ) : NotificationRepository {
        private val inbox = MutableSharedFlow<List<PostNotification>>(extraBufferCapacity = 8)
        var markCallCount = 0
            private set
        var markAllCallCount = 0
            private set

        suspend fun emit(items: List<PostNotification>) {
            inbox.emit(items)
        }

        override fun getNewsfeed(
            category: NotificationCategory,
        ): Flow<List<PostNotification>> = inbox

        override suspend fun getNotificationDetail(
            notificationId: String,
        ): Result<PostNotification?> = Result.success(detail)

        override fun getInbox(): Flow<List<PostNotification>> = inbox

        override suspend fun markInboxNotificationRead(notificationId: String): Result<Unit> {
            markCallCount++
            val result = markResults.removeFirst()
            markGates.removeFirstOrNull()?.await()
            return result
        }

        override suspend fun markAllInboxNotificationsRead(): Result<Unit> {
            markAllCallCount++
            val result = markAllResults.removeFirst()
            markAllGates.removeFirstOrNull()?.await()
            return result
        }

        override suspend fun getInboxNotification(
            notificationId: String,
        ): Result<PostNotification?> = detailResults.removeFirstOrNull()?.await() ?: Result.success(detail)
    }

    private class FakeUserNotificationRepository : UserNotificationRepository {
        override val unreadCount = MutableStateFlow(0)
        override val currentUserId: String = "user-1"
        var scheduledRefreshCalls = 0
            private set

        override fun getNotifications(): Flow<List<NotificationItem>> = flowOf(emptyList())

        override suspend fun refreshUnreadCount(): Result<Int> = Result.success(0)

        override fun scheduleUnreadCountRefresh() {
            scheduledRefreshCalls++
        }

        override fun clearUnreadCount() = Unit

        override suspend fun markAsRead(notificationId: String): Result<Boolean> = Result.success(false)
    }
}
