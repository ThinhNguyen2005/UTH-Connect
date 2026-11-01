package com.giathinh.uthconnect.presentation.events.detail

import com.giathinh.uthconnect.data.repository.StaticEventRepository
import com.giathinh.uthconnect.domain.model.Event
import com.giathinh.uthconnect.domain.repository.EventRepository
import com.giathinh.uthconnect.domain.repository.StudentIdProvider
import com.giathinh.uthconnect.domain.usecase.GetEventsUseCase
import com.giathinh.uthconnect.domain.usecase.RegisterEventUseCase
import com.giathinh.uthconnect.testing.MainDispatcherRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class EventDetailViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `loads event and enables registration when seats are available`() {
        val viewModel = createViewModel()
        viewModel.load("career-day")

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals("career-day", viewModel.uiState.value.event?.id)
        assertTrue(viewModel.uiState.value.canRegister)
    }

    @Test
    fun `registration shows its ID and disables the button`() {
        val viewModel = createViewModel()
        viewModel.load("career-day")

        viewModel.onAction(EventDetailAction.Register)

        assertEquals("2023040123", viewModel.uiState.value.registration?.studentId)
        assertNotNull(viewModel.uiState.value.registration?.id)
        assertFalse(viewModel.uiState.value.canRegister)
    }

    @Test
    fun `closed event cannot be registered`() {
        val viewModel = createViewModel()
        viewModel.load("green-campus")

        assertFalse(viewModel.uiState.value.canRegister)
        viewModel.onAction(EventDetailAction.Register)
        assertEquals(null, viewModel.uiState.value.registration)
    }

    @Test
    fun `load error can be retried`() {
        var shouldFail = true
        val repository = object : EventRepository {
            override suspend fun events(): Result<List<Event>> =
                if (shouldFail) Result.failure(IllegalStateException("temporary error"))
                else StaticEventRepository().events()
        }
        val viewModel = createViewModel(eventRepository = repository)

        viewModel.load("career-day")
        assertEquals(EventDetailFailure.LoadFailed, viewModel.uiState.value.failure)

        shouldFail = false
        viewModel.onAction(EventDetailAction.Retry)
        assertEquals("career-day", viewModel.uiState.value.event?.id)
        assertEquals(null, viewModel.uiState.value.failure)
    }

    @Test
    fun `registration error is shown and can be retried`() {
        val studentIdProvider = object : StudentIdProvider {
            override suspend fun studentId(): Result<String> =
                Result.failure(IllegalStateException("profile is not available"))
        }
        val viewModel = createViewModel(studentIdProvider = studentIdProvider)
        viewModel.load("career-day")

        viewModel.onAction(EventDetailAction.Register)

        assertTrue(viewModel.uiState.value.registrationFailed)
        assertTrue(viewModel.uiState.value.canRegister)
    }

    private fun createViewModel(
        eventRepository: EventRepository = StaticEventRepository(),
        studentIdProvider: StudentIdProvider = object : StudentIdProvider {
            override suspend fun studentId() = Result.success("2023040123")
        },
    ): EventDetailViewModel {
        val events = GetEventsUseCase(eventRepository)
        return EventDetailViewModel(events, RegisterEventUseCase(events, studentIdProvider))
    }
}
