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
            markResults = ArrayDeque(listOf(Result.success(Unit), Result.failure(IllegalStateException("temporary")))),
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
        private val markGates: ArrayDeque<CompletableDeferred<Unit>> = ArrayDeque(),
        private val detail: PostNotification? = null,
    ) : NotificationRepository {
        private val inbox = MutableSharedFlow<List<PostNotification>>(extraBufferCapacity = 8)

        suspend fun emit(items: List<PostNotification>) {
            inbox.emit(items)
        }

        override fun getNewsfeed(category: NotificationCategory): Flow<List<PostNotification>> = inbox

        override suspend fun getNotificationDetail(notificationId: String): Result<PostNotification?> = Result.success(detail)

        override fun getInbox(): Flow<List<PostNotification>> = inbox

        override suspend fun markInboxNotificationRead(notificationId: String): Result<Unit> {
            markGates.removeFirstOrNull()?.await()
            return markResults.removeFirst()
        }

        override suspend fun getInboxNotification(notificationId: String): Result<PostNotification?> = Result.success(detail)
    }
}
