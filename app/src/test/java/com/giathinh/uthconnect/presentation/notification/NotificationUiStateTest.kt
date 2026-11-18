package com.giathinh.uthconnect.presentation.notification

import com.giathinh.uthconnect.domain.model.NotificationCategory
import com.giathinh.uthconnect.domain.model.PostNotification
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationUiStateTest {
    private val academic = notification("academic", NotificationCategory.ACADEMIC)
    private val exam = notification("exam", NotificationCategory.EXAMINATION)
    private val event = notification("event", NotificationCategory.ACTIVITIES)
    private val system = notification("system", NotificationCategory.GENERAL)

    @Test
    fun `study filter includes academic and examination notifications`() {
        val state = NotificationUiState(notifications = listOf(academic, exam, event, system))

        val filtered = state.copy(selectedCategory = NotificationCategory.ACADEMIC).filteredNotifications

        assertEquals(listOf(academic, exam), filtered)
    }

    @Test
    fun `system filter includes general and tuition notifications`() {
        val tuition = notification("tuition", NotificationCategory.TUITION)
        val state = NotificationUiState(notifications = listOf(system, tuition, event))

        val filtered = state.copy(selectedCategory = NotificationCategory.GENERAL).filteredNotifications

        assertEquals(listOf(system, tuition), filtered)
    }

    @Test
    fun `category counts follow the same grouping as filters`() {
        val tuition = notification("tuition", NotificationCategory.TUITION)
        val state = NotificationUiState(notifications = listOf(academic, exam, event, system, tuition))

        assertEquals(2, state.countForCategory(NotificationCategory.ACADEMIC))
        assertEquals(2, state.countForCategory(NotificationCategory.GENERAL))
        assertEquals(5, state.countForCategory(null))
    }

    @Test
    fun `unread count excludes persisted read ids`() {
        val state = NotificationUiState(
            notifications = listOf(academic, event),
            readNotificationIds = setOf(academic.id),
        )

        assertEquals(1, state.totalUnreadCount)
        assertTrue(state.previousNotifications.all { it.id in setOf(academic.id, event.id) })
    }

    private fun notification(id: String, category: NotificationCategory) = PostNotification(
        id = id,
        title = id,
        summary = "summary",
        content = "content",
        category = category,
        author = "UTH Connect",
        publishedAt = 1_577_836_800_000L,
    )
}
