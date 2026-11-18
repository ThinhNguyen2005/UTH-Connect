package com.giathinh.uthconnect.presentation.notificationshub

import com.giathinh.uthconnect.domain.model.NotificationItem
import com.giathinh.uthconnect.domain.model.UserNotificationType
import com.giathinh.uthconnect.domain.repository.UserNotificationRepository
import com.giathinh.uthconnect.testing.MainDispatcherRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class UserNotificationsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(StandardTestDispatcher())

    @Test
    fun `old GET does not restore optimistic read after PATCH failure`() = runTest {
        val item = notification()
        val staleResponse = CompletableDeferred<List<NotificationItem>>()
        val repository = FakeUserNotificationRepository(
            initial = listOf(item),
            markResult = Result.failure(IllegalStateException("temporary")),
            staleResponse = staleResponse,
        )
        val viewModel = UserNotificationsViewModel(repository)
        advanceUntilIdle()

        viewModel.onAction(UserNotificationsAction.Refresh)
        runCurrent()
        viewModel.onAction(UserNotificationsAction.Open(item))
        advanceUntilIdle()
        staleResponse.complete(listOf(item))
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.notifications.single().isRead)
    }

    @Test
    fun `successful no-op PATCH is reconciled instead of confirmed optimistically`() = runTest {
        val item = notification()
        val repository = FakeUserNotificationRepository(
            initial = listOf(item),
            markResult = Result.success(false),
        )
        val viewModel = UserNotificationsViewModel(repository)
        advanceUntilIdle()

        viewModel.onAction(UserNotificationsAction.Open(item))
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.notifications.single().isRead)
    }

    @Test
    fun `stale reconcile does not overwrite a newer read attempt`() = runTest {
        val item = notification()
        val staleResponse = CompletableDeferred<List<NotificationItem>>()
        val secondPatch = CompletableDeferred<Unit>()
        val repository = FakeUserNotificationRepository(
            initial = listOf(item),
            markResults = ArrayDeque(
                listOf(
                    Result.failure(IllegalStateException("temporary")),
                    Result.success(true),
                ),
            ),
            staleResponse = staleResponse,
            markGates = mapOf(2 to secondPatch),
        )
        val viewModel = UserNotificationsViewModel(repository)
        advanceUntilIdle()

        viewModel.onAction(UserNotificationsAction.Open(item))
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.notifications.single().isRead)

        viewModel.onAction(UserNotificationsAction.Open(item))
        runCurrent()
        staleResponse.complete(listOf(item))
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.notifications.single().isRead)
        secondPatch.complete(Unit)
        advanceUntilIdle()
    }

    @Test
    fun `stale server read confirmation survives a newer failed read attempt`() = runTest {
        val item = notification()
        val staleResponse = CompletableDeferred<List<NotificationItem>>()
        val secondPatch = CompletableDeferred<Unit>()
        val repository = FakeUserNotificationRepository(
            initial = listOf(item),
            markResults = ArrayDeque(
                listOf(
                    Result.failure(IllegalStateException("first attempt")),
                    Result.failure(IllegalStateException("second attempt")),
                ),
            ),
            staleResponse = staleResponse,
            markGates = mapOf(2 to secondPatch),
        )
        val viewModel = UserNotificationsViewModel(repository)
        advanceUntilIdle()

        viewModel.onAction(UserNotificationsAction.Open(item))
        runCurrent()
        viewModel.onAction(UserNotificationsAction.Open(item))
        runCurrent()

        secondPatch.complete(Unit)
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.notifications.single().isRead)

        staleResponse.complete(listOf(item.copy(isRead = true)))
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.notifications.single().isRead)
    }

    @Test
    fun `stale read confirmation does not clear a newer inbox refresh error`() = runTest {
        val item = notification()
        val reconcileResponse = CompletableDeferred<List<NotificationItem>>()
        val repository = FakeUserNotificationRepository(
            initial = listOf(item),
            markResult = Result.failure(IllegalStateException("read failed")),
            staleResponse = reconcileResponse,
            failedGetRequest = 2,
        )
        val viewModel = UserNotificationsViewModel(repository)
        advanceUntilIdle()

        viewModel.onAction(UserNotificationsAction.Open(item))
        runCurrent()
        viewModel.onAction(UserNotificationsAction.Refresh)
        advanceUntilIdle()
        assertEquals("refresh failed", viewModel.uiState.value.errorMessage)

        reconcileResponse.complete(listOf(item.copy(isRead = true)))
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.notifications.single().isRead)
        assertEquals("refresh failed", viewModel.uiState.value.errorMessage)
    }

    private fun notification() = NotificationItem(
        id = "notification-1",
        recipientId = "user-1",
        title = "System update",
        content = "Details",
        type = UserNotificationType.SYSTEM,
        isRead = false,
        createdAt = 1L,
    )

    private class FakeUserNotificationRepository(
        private val initial: List<NotificationItem>,
        private val markResult: Result<Boolean> = Result.success(true),
        private val markResults: ArrayDeque<Result<Boolean>> = ArrayDeque(),
        private val staleResponse: CompletableDeferred<List<NotificationItem>>? = null,
        private val markGates: Map<Int, CompletableDeferred<Unit>> = emptyMap(),
        private val failedGetRequest: Int? = null,
    ) : UserNotificationRepository {
        override val unreadCount = MutableStateFlow(0)
        override val currentUserId: String = "user-1"
        private var getCount = 0
        private var markCount = 0

        override fun getNotifications(): Flow<List<NotificationItem>> {
            val request = getCount++
            return if (request == failedGetRequest) {
                flow { throw IllegalStateException("refresh failed") }
            } else if (request == 1 && staleResponse != null) {
                flow { emit(staleResponse.await()) }
            } else {
                flowOf(initial)
            }
        }

        override suspend fun refreshUnreadCount(): Result<Int> = Result.success(0)

        override fun clearUnreadCount() = Unit

        override suspend fun markAsRead(notificationId: String): Result<Boolean> {
            markCount++
            markGates[markCount]?.await()
            return nextMarkResult()
        }

        private fun nextMarkResult() = markResults.removeFirstOrNull() ?: markResult
    }
}
