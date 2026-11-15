package com.giathinh.uthconnect.presentation.notification

import com.giathinh.uthconnect.domain.model.NotificationCategory
import com.giathinh.uthconnect.domain.model.PostNotification
import com.giathinh.uthconnect.domain.repository.NotificationRepository
import com.giathinh.uthconnect.testing.MainDispatcherRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
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
        val viewModel = NotificationViewModel(repository)

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
        val viewModel = NotificationViewModel(repository)

        advanceUntilIdle()
        viewModel.onAction(NotificationAction.LoadDetail(notification.id))
        viewModel.onAction(NotificationAction.LoadDetail(notification.id))
        advanceUntilIdle()

        firstPatch.complete(Unit)
        advanceUntilIdle()
        secondPatch.complete(Unit)
        advanceUntilIdle()

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
        val viewModel = NotificationViewModel(repository)

        viewModel.onAction(NotificationAction.LoadDetail(notification.id))
        viewModel.onAction(NotificationAction.LoadDetail(notification.id))
        advanceUntilIdle()

        firstPatch.complete(Unit)
        advanceUntilIdle()
        secondPatch.complete(Unit)
        advanceUntilIdle()

        assertTrue(notification.id !in viewModel.uiState.value.readNotificationIds)
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
        val viewModel = NotificationViewModel(repository)
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
        val viewModel = NotificationViewModel(repository)
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
    fun `stale same detail failure does not end the latest loading request`() = runTest {
        val notification = notification("notification-detail-generation")
        val staleResult = CompletableDeferred<Result<PostNotification?>>()
        val latestResult = CompletableDeferred<Result<PostNotification?>>()
        val repository = FakeNotificationRepository(
            markResults = ArrayDeque(listOf(Result.success(Unit))),
            detailResults = ArrayDeque(listOf(staleResult, latestResult)),
        )
        val viewModel = NotificationViewModel(repository)

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
            markGates.removeFirstOrNull()?.await()
            return markResults.removeFirst()
        }

        override suspend fun markAllInboxNotificationsRead(): Result<Unit> {
            markAllCallCount++
            return markAllResults.removeFirst()
        }

        override suspend fun getInboxNotification(
            notificationId: String,
        ): Result<PostNotification?> = detailResults.removeFirstOrNull()?.await() ?: Result.success(detail)
    }
}
