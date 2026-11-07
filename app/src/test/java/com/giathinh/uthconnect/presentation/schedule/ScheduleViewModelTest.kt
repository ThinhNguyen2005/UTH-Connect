package com.giathinh.uthconnect.presentation.schedule

import androidx.lifecycle.viewModelScope
import com.giathinh.uthconnect.data.repository.InMemoryTimetableRepository
import com.giathinh.uthconnect.domain.usecase.TimetableUseCase
import com.giathinh.uthconnect.testing.MainDispatcherRule
import kotlinx.coroutines.cancel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class ScheduleViewModelTest {
    @get:Rule val dispatcherRule = MainDispatcherRule()

    private val clock = Clock.fixed(Instant.parse("2026-10-31T17:30:00Z"), ZoneId.of("Asia/Ho_Chi_Minh"))

    @Test fun `initial events list is empty without sample data`() {
        val vm = ScheduleViewModel(TimetableUseCase(InMemoryTimetableRepository(clock)), clock)
        try {
            assertTrue(vm.uiState.value.events.isEmpty())
        } finally {
            vm.viewModelScope.cancel()
        }
    }

    @Test fun `cannot register for an event that has already started`() {
        val eventClock = Clock.fixed(Instant.parse("2026-10-31T11:31:00Z"), ZoneId.of("Asia/Ho_Chi_Minh"))
        val vm = ScheduleViewModel(TimetableUseCase(InMemoryTimetableRepository(eventClock)), eventClock)
        try {
            val pastDate = LocalDate.of(2026, 10, 30)
            vm.onAction(
                ScheduleAction.CreateWorkshopEvent(
                    title = "Past Workshop",
                    category = "Hội thảo",
                    coOrganizers = listOf("Đoàn Hội"),
                    summary = "Tóm tắt",
                    date = pastDate,
                    startTime = "08:00",
                    endTime = "10:00",
                    venue = "A.101",
                    trainingPoints = "+5 ĐRL",
                    managers = listOf("GV"),
                ),
            )
            val pastEvent = vm.uiState.value.events.first { it.title == "Past Workshop" }
            assertTrue(vm.uiState.value.now >= pastEvent.startAt)
            vm.onAction(ScheduleAction.RegisterEvent(pastEvent.id))
            assertEquals(EventRegistration.NOT_REGISTERED, vm.uiState.value.events.first { it.id == pastEvent.id }.registration)
        } finally {
            vm.viewModelScope.cancel()
        }
    }

    @Test fun `can register for an event that has not started`() {
        val vm = ScheduleViewModel(TimetableUseCase(InMemoryTimetableRepository(clock)), clock)
        try {
            val futureDate = LocalDate.of(2026, 11, 20)
            vm.onAction(
                ScheduleAction.CreateWorkshopEvent(
                    title = "Upcoming Workshop",
                    category = "Hội thảo",
                    coOrganizers = listOf("Đoàn Hội"),
                    summary = "Tóm tắt",
                    date = futureDate,
                    startTime = "08:00",
                    endTime = "10:00",
                    venue = "A.101",
                    trainingPoints = "+5 ĐRL",
                    managers = listOf("GV"),
                ),
            )
            val upcoming = vm.uiState.value.events.first { it.title == "Upcoming Workshop" }
            assertTrue(vm.uiState.value.now < upcoming.startAt)
            vm.onAction(ScheduleAction.RegisterEvent(upcoming.id))
            assertEquals(EventRegistration.REGISTERED, vm.uiState.value.events.first { it.id == upcoming.id }.registration)
        } finally {
            vm.viewModelScope.cancel()
        }
    }

    @Test fun `deadline form reports field errors and clears errors on correction`() {
        val repository = InMemoryTimetableRepository(clock)
        val vm = ScheduleViewModel(TimetableUseCase(repository), clock)
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

    @Test fun `teacher can open, close and toggle teacher mode`() {
        val vm = ScheduleViewModel(TimetableUseCase(InMemoryTimetableRepository(clock)), clock)
        try {
            assertFalse(vm.uiState.value.isTeacher)
            vm.onAction(ScheduleAction.ToggleTeacherMode)
            assertTrue(vm.uiState.value.isTeacher)

            assertFalse(vm.uiState.value.showCreateEvent)
            vm.onAction(ScheduleAction.OpenCreateEvent)
            assertTrue(vm.uiState.value.showCreateEvent)

            vm.onAction(ScheduleAction.CloseCreateEvent)
            assertFalse(vm.uiState.value.showCreateEvent)
        } finally {
            vm.viewModelScope.cancel()
        }
    }

    @Test fun `create workshop event adds event and closes creation screen`() {
        val vm = ScheduleViewModel(TimetableUseCase(InMemoryTimetableRepository(clock)), clock)
        try {
            vm.onAction(ScheduleAction.OpenCreateEvent)
            assertTrue(vm.uiState.value.showCreateEvent)

            val eventDate = LocalDate.of(2026, 11, 15)
            vm.onAction(
                ScheduleAction.CreateWorkshopEvent(
                    title = "Workshop: AI & IoT",
                    category = "Hội thảo / Workshop",
                    coOrganizers = listOf("Đoàn - Hội CNTT", "CLB Sáng tạo Trẻ"),
                    summary = "Tóm tắt sự kiện",
                    date = eventDate,
                    startTime = "08:30",
                    endTime = "11:30",
                    venue = "Hội trường A.101",
                    trainingPoints = "+5 ĐRL",
                    managers = listOf("ThS. Trần Thu Trang"),
                ),
            )

            assertFalse(vm.uiState.value.showCreateEvent)
            assertEquals(eventDate, vm.uiState.value.selectedDate)
            val created = vm.uiState.value.events.first()
            assertEquals("Workshop: AI & IoT", created.title)
            assertEquals("Hội trường A.101", created.location)
            assertEquals("+5 ĐRL", created.trainingPoints)
        } finally {
            vm.viewModelScope.cancel()
        }
    }
}
