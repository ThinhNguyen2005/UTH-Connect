package com.giathinh.uthconnect.presentation.schedule

import com.giathinh.uthconnect.domain.model.CourseSchedule
import com.giathinh.uthconnect.domain.model.Deadline
import com.giathinh.uthconnect.domain.repository.TimetableRepository
import com.giathinh.uthconnect.domain.usecase.TimetableUseCases
import com.giathinh.uthconnect.testing.MainDispatcherRule
import java.time.LocalDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ScheduleViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `local entries stream into state and can be edited or deleted`() {
        val repository = FakeTimetableRepository()
        val viewModel = ScheduleViewModel(TimetableUseCases(repository))
        val schedule = CourseSchedule(
            id = "class-1", courseName = "Mobile Development", courseCode = "CS101",
            lecturer = "", room = "B102", campus = "Campus 1", dayOfWeek = 1,
            startPeriod = 1, endPeriod = 3, startTime = "07:00", endTime = "09:15", credits = 3,
        )
        val deadline = Deadline("deadline-1", "Project", "CS101", LocalDate.of(2026, 11, 20), "Submit online")

        viewModel.onAction(ScheduleAction.SaveSchedule(schedule))
        viewModel.onAction(ScheduleAction.SaveDeadline(deadline))
        assertEquals(listOf(schedule), viewModel.uiState.value.schedules)
        assertEquals(listOf(deadline), viewModel.uiState.value.deadlines)

        val editedSchedule = schedule.copy(room = "C203")
        viewModel.onAction(ScheduleAction.SaveSchedule(editedSchedule))
        assertEquals(listOf(editedSchedule), viewModel.uiState.value.schedules)

        viewModel.onAction(ScheduleAction.DeleteSchedule(schedule.id))
        viewModel.onAction(ScheduleAction.DeleteDeadline(deadline.id))
        assertTrue(viewModel.uiState.value.schedules.isEmpty())
        assertTrue(viewModel.uiState.value.deadlines.isEmpty())
        assertNull(viewModel.uiState.value.failure)
    }

    @Test
    fun `date and view actions update selected week`() {
        val viewModel = ScheduleViewModel(TimetableUseCases(FakeTimetableRepository()))
        val today = viewModel.uiState.value.selectedDate

        viewModel.onAction(ScheduleAction.NextWeek)
        viewModel.onAction(ScheduleAction.SetWeekView(true))
        viewModel.onAction(ScheduleAction.ShowDeadlines)

        assertEquals(today.plusWeeks(1), viewModel.uiState.value.selectedDate)
        assertTrue(viewModel.uiState.value.isWeekView)
        assertEquals(ScheduleTab.Deadlines, viewModel.uiState.value.selectedTab)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    private class FakeTimetableRepository : TimetableRepository {
        private val schedules = MutableStateFlow(emptyList<CourseSchedule>())
        private val deadlines = MutableStateFlow(emptyList<Deadline>())

        override fun observeSchedules(): Flow<List<CourseSchedule>> = schedules
        override fun observeDeadlines(): Flow<List<Deadline>> = deadlines

        override suspend fun saveSchedule(schedule: CourseSchedule): Result<Unit> {
            schedules.update { current -> current.filterNot { it.id == schedule.id } + schedule }
            return Result.success(Unit)
        }

        override suspend fun deleteSchedule(id: String): Result<Unit> {
            schedules.update { current -> current.filterNot { it.id == id } }
            return Result.success(Unit)
        }

        override suspend fun saveDeadline(deadline: Deadline): Result<Unit> {
            deadlines.update { current -> current.filterNot { it.id == deadline.id } + deadline }
            return Result.success(Unit)
        }

        override suspend fun deleteDeadline(id: String): Result<Unit> {
            deadlines.update { current -> current.filterNot { it.id == id } }
            return Result.success(Unit)
        }

        override suspend fun refreshReminders(): Result<Unit> = Result.success(Unit)
    }
}
