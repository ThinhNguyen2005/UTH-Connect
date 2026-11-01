package com.giathinh.uthconnect.domain.usecase

import com.giathinh.uthconnect.data.repository.StaticEventRepository
import com.giathinh.uthconnect.domain.model.Campus
import com.giathinh.uthconnect.domain.model.Event
import com.giathinh.uthconnect.domain.model.EventStatus
import com.giathinh.uthconnect.domain.repository.EventRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class GetEventsUseCaseTest {
    private val repository = StaticEventRepository()
    private val getEvents = GetEventsUseCase(repository)

    @Test
    fun `repository co dung 6 su kien co dinh va ID khong trung`() = runTest {
        val events = repository.events().getOrThrow()

        assertEquals(6, events.size)
        assertEquals(6, events.map { it.id }.distinct().size)
        assertEquals(events, repository.events().getOrThrow())
        assertEquals(Campus.entries.toSet(), events.map { it.campus }.toSet())
        assertEquals(EventStatus.entries.toSet(), events.map { it.status }.toSet())
        assertTrue(events.all { it.capacity > 0 && it.registeredCount in 0..it.capacity })
    }

    @Test
    fun `null tra tat ca su kien va sap xep theo thoi gian`() = runTest {
        val events = getEvents().getOrThrow()

        assertEquals(6, events.size)
        assertEquals(events.sortedBy { it.startAt }, events)
    }

    @Test
    fun `loc tung co so chi tra su kien thuoc co so do`() = runTest {
        for (campus in Campus.entries) {
            val events = getEvents(campus).getOrThrow()
            assertEquals(2, events.size)
            assertTrue(events.all { it.campus == campus })
        }
    }

    @Test
    fun `loi repository duoc giu nguyen qua usecase`() = runTest {
        val error = IllegalStateException("Không tải được sự kiện")
        val failedRepository = object : EventRepository {
            override suspend fun events(): Result<List<Event>> = Result.failure(error)
        }

        assertSame(error, GetEventsUseCase(failedRepository)(Campus.THU_DUC).exceptionOrNull())
    }

    @Test
    fun `chi OPEN va con cho moi duoc dang ky`() = runTest {
        val event = repository.events().getOrThrow().first()
        assertTrue(event.canRegister)
        assertEquals(55, event.remainingSeats)
        assertFalse(event.copy(registeredCount = event.capacity).canRegister)

        for (status in EventStatus.entries.filter { it != EventStatus.OPEN }) {
            assertFalse(event.copy(status = status, registeredCount = 0).canRegister)
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `khong chap nhan so dang ky vuot suc chua`() = runTest {
        val event = repository.events().getOrThrow().first()
        event.copy(registeredCount = event.capacity + 1)
        Unit
    }
}
