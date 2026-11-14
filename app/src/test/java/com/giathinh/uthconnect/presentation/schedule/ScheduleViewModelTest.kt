package com.giathinh.uthconnect.presentation.schedule

import androidx.lifecycle.viewModelScope
import com.giathinh.uthconnect.data.repository.InMemoryTimetableRepository
import com.giathinh.uthconnect.domain.model.Event
import com.giathinh.uthconnect.domain.model.StudentProfile
import com.giathinh.uthconnect.domain.repository.EventRepository
import com.giathinh.uthconnect.domain.repository.SessionRepository
import com.giathinh.uthconnect.domain.repository.StudentSession
import com.giathinh.uthconnect.domain.repository.TicketRepository
import com.giathinh.uthconnect.domain.usecase.TimetableUseCase
import com.giathinh.uthconnect.testing.MainDispatcherRule
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class ScheduleViewModelTest {
    @get:Rule val dispatcherRule = MainDispatcherRule()

    private val clock = Clock.fixed(Instant.parse("2026-10-31T17:30:00Z"), ZoneId.of("Asia/Ho_Chi_Minh"))

    /** Verifies deadline validation errors appear and clear as fields are corrected. */
    @Test fun `deadline form reports field errors and clears errors on correction`() {
        val repository = InMemoryTimetableRepository(clock)
        val vm = ScheduleViewModel(
            TimetableUseCase(repository),
            EmptyEventRepository,
            EmptyTicketRepository,
            EmptySessionRepository,
            clock,
        )
        try {
            vm.onAction(ScheduleAction.OpenForm)
            vm.onAction(ScheduleAction.Date("2026-02-30"))
            vm.onAction(ScheduleAction.Time("25:65"))
            vm.onAction(ScheduleAction.Save)

            assertTrue(vm.uiState.value.showForm)
            assertTrue(vm.uiState.value.titleError)
            assertTrue(vm.uiState.value.dateError)
            assertTrue(vm.uiState.value.timeError)
            assertEquals(4, repository.timetable.value.deadlines.size)

            vm.onAction(ScheduleAction.Title("Nộp đồ án"))
            vm.onAction(ScheduleAction.Date("2026-11-02"))
            vm.onAction(ScheduleAction.Time("15:30"))
            assertFalse(vm.uiState.value.titleError)
            assertFalse(vm.uiState.value.dateError)
            assertFalse(vm.uiState.value.timeError)

            vm.onAction(ScheduleAction.Save)
            assertFalse(vm.uiState.value.showForm)
            assertEquals(5, repository.timetable.value.deadlines.size)
            assertEquals("Nộp đồ án", repository.timetable.value.deadlines.last().title)
            assertEquals(LocalDateTime.of(2026, 11, 2, 15, 30), repository.timetable.value.deadlines.last().dueAt)
        } finally {
            vm.viewModelScope.cancel()
        }
    }

    /** Exposes a registration lookup failure while the collector waits for its retry. */
    @Test fun `registration lookup failure is surfaced in ui state`() {
        val repository = object : EventRepository by EmptyEventRepository {
            override fun currentUserRegistrations(): Flow<Set<String>> = flow {
                error("offline")
            }
        }
        val vm = ScheduleViewModel(
            TimetableUseCase(InMemoryTimetableRepository(clock)),
            repository,
            EmptyTicketRepository,
            EmptySessionRepository,
            clock,
        )
        try {
            assertTrue(vm.uiState.value.registrationsLoadFailed)
            assertFalse(vm.uiState.value.isRegistrationsLoading)
        } finally {
            vm.viewModelScope.cancel()
        }
    }

    /** Keeps the registrations stream pending and verifies initial loading is exposed. */
    @Test fun `registration status starts loading before first emission`() {
        val repository = object : EventRepository by EmptyEventRepository {
            override fun currentUserRegistrations(): Flow<Set<String>> = flow {
                awaitCancellation()
            }
        }
        val vm = ScheduleViewModel(
            TimetableUseCase(InMemoryTimetableRepository(clock)),
            repository,
            EmptyTicketRepository,
            EmptySessionRepository,
            clock,
        )
        try {
            assertTrue(vm.uiState.value.isRegistrationsLoading)
            assertFalse(vm.uiState.value.registrationsLoadFailed)
        } finally {
            vm.viewModelScope.cancel()
        }
    }

    /** Prevents registration requests when an event has already started. */
    @Test fun `registration is rejected at event start time`() {
        val event = Event(
            id = "started-event",
            title = "Started event",
            description = "",
            orgId = "org",
            location = "Campus",
            campus = "UTH",
            startAt = clock.millis() - 60_000,
            endAt = clock.millis() + 60_000,
            capacity = 20,
        )
        var registrationCalls = 0
        val repository = object : EventRepository by EmptyEventRepository {
            override fun getEvents(campus: String?): Flow<List<Event>> = flowOf(listOf(event))
            override fun currentUserRegistrations(): Flow<Set<String>> = flowOf(emptySet())
            override suspend fun registerCurrentUser(eventId: String): Result<Unit> {
                registrationCalls++
                return Result.success(Unit)
            }
        }
        val vm = ScheduleViewModel(
            TimetableUseCase(InMemoryTimetableRepository(clock)),
            repository,
            EmptyTicketRepository,
            EmptySessionRepository,
            clock,
        )
        try {
            assertFalse(vm.uiState.value.isRegistrationsLoading)
            assertEquals(1, vm.uiState.value.events.size)
            vm.onAction(ScheduleAction.RegisterEvent(event.id))
            assertEquals(0, registrationCalls)
        } finally {
            vm.viewModelScope.cancel()
        }
    }

    private val emptyEvents = object : EventRepository {
        override fun getEvents(campus: String?): Flow<List<Event>> = flowOf(emptyList())
        override suspend fun getEventDetail(eventId: String): Result<Event?> = Result.success(null)
        override suspend fun registerEvent(eventId: String, uid: String, studentId: String, studentName: String): Result<Unit> = Result.failure(UnsupportedOperationException())
        override suspend fun cancelRegistration(eventId: String, uid: String): Result<Unit> = Result.failure(UnsupportedOperationException())
        override fun isRegistered(eventId: String, uid: String): Flow<Boolean> = flowOf(false)
    }
}

private object EmptyEventRepository : EventRepository {
    override fun getEvents(campus: String?): Flow<List<Event>> = flowOf(emptyList())
    override suspend fun getEventDetail(eventId: String): Result<Event?> = Result.success(null)
    override suspend fun registerEvent(
        eventId: String,
        uid: String,
        studentId: String,
        studentName: String,
    ): Result<Unit> = Result.success(Unit)
    override suspend fun cancelRegistration(eventId: String, uid: String): Result<Unit> = Result.success(Unit)
    override fun isRegistered(eventId: String, uid: String): Flow<Boolean> = flowOf(false)
    override fun currentUserRegistrations(): Flow<Set<String>> = flowOf(emptySet())
}

private object EmptyTicketRepository : TicketRepository {
    override suspend fun getTickets(studentId: String) = Result.success(
        emptyList<com.giathinh.uthconnect.domain.model.Ticket>(),
    )
}

private object EmptySessionRepository : SessionRepository {
    override val session = MutableStateFlow(StudentSession(isRestoring = false))
    override suspend fun restore() = Unit
    override suspend fun start(profile: StudentProfile, remember: Boolean) = Unit
    override suspend fun updateProfile(profile: StudentProfile) = Unit
    override suspend fun enableBiometric(studentId: String) = Unit
    override suspend fun disableBiometric() = Unit
    override suspend fun signOut() = Unit
}
