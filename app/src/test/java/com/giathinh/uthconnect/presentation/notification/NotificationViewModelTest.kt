package com.giathinh.uthconnect.presentation.notification

import com.giathinh.uthconnect.domain.model.NotificationCategory
import com.giathinh.uthconnect.domain.model.PostNotification
import com.giathinh.uthconnect.domain.repository.NotificationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class NotificationViewModelTest {
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
    ) : NotificationRepository {
        private val inbox = MutableSharedFlow<List<PostNotification>>(extraBufferCapacity = 8)

        suspend fun emit(items: List<PostNotification>) {
            inbox.emit(items)
        }

        override fun getNewsfeed(category: NotificationCategory): Flow<List<PostNotification>> = inbox

        override suspend fun getNotificationDetail(notificationId: String): Result<PostNotification?> = Result.success(null)

        override fun getInbox(): Flow<List<PostNotification>> = inbox

        override suspend fun markInboxNotificationRead(notificationId: String): Result<Unit> = markResults.removeFirst()
    }
}
