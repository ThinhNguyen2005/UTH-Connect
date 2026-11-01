package com.giathinh.uthconnect.presentation.schedule

import androidx.lifecycle.viewModelScope
import com.giathinh.uthconnect.data.repository.InMemoryTimetableRepository
import com.giathinh.uthconnect.domain.usecase.TimetableUseCase
import com.giathinh.uthconnect.testing.MainDispatcherRule
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import kotlinx.coroutines.cancel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ScheduleViewModelTest {
    @get:Rule val dispatcherRule = MainDispatcherRule()

    private val clock = Clock.fixed(Instant.parse("2026-10-31T17:30:00Z"), ZoneId.of("Asia/Ho_Chi_Minh"))

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
}
