package com.giathinh.uthconnect.presentation.events

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.giathinh.uthconnect.domain.model.Event
import com.giathinh.uthconnect.domain.model.EventDraft
import com.giathinh.uthconnect.domain.model.EventStatus
import com.giathinh.uthconnect.domain.model.UserRole
import com.giathinh.uthconnect.domain.repository.EventRepository
import com.giathinh.uthconnect.domain.repository.TicketRepository
import com.giathinh.uthconnect.domain.usecase.GetProfileUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EventsUiState(
    val events: List<Event> = emptyList(),
    val registeredIds: Set<String> = emptySet(),
    val role: UserRole = UserRole.STUDENT,
    val currentUserId: String? = null,
    val loading: Boolean = true,
    val busyEvent: String? = null,
    val checkingIn: Boolean = false,
    val message: String? = null,
    val error: String? = null,
)

@HiltViewModel
class EventsViewModel @Inject constructor(
    private val events: EventRepository,
    private val tickets: TicketRepository,
    getProfile: GetProfileUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(EventsUiState())
    val state: StateFlow<EventsUiState> = _state.asStateFlow()
    private var eventJob: Job? = null
    private var registrationJob: Job? = null

    init {
        _state.update { it.copy(currentUserId = events.currentUserId()) }
        viewModelScope.launch {
            getProfile.observe().collect { profile ->
                _state.update { it.copy(role = profile?.role ?: UserRole.STUDENT) }
            }
        }
        reload()
    }

    fun reload() {
        eventJob?.cancel()
        registrationJob?.cancel()
        _state.update { it.copy(loading = true, error = null) }
        eventJob = viewModelScope.launch {
            events.getEvents().catch { error ->
                if (error is CancellationException) throw error
                _state.update { it.copy(loading = false, error = error.message ?: "Không thể tải sự kiện") }
            }.collect { result ->
                _state.update { it.copy(loading = false, events = result, error = null) }
            }
        }
        registrationJob = viewModelScope.launch {
            events.currentUserRegistrations().catch { error ->
                if (error is CancellationException) throw error
                _state.update { it.copy(error = error.message ?: "Không thể tải đăng ký") }
            }.collect { ids -> _state.update { it.copy(registeredIds = ids) } }
        }
        events.refresh()
    }

    fun register(eventId: String) = mutate(eventId) { events.registerCurrentUser(eventId) }

    fun cancel(eventId: String) = mutate(eventId) { events.cancelCurrentRegistration(eventId) }

    fun saveEvent(eventId: String?, draft: EventDraft) =
        mutate(eventId ?: "new", "Đã lưu sự kiện") { events.saveEvent(eventId, draft) }

    fun setStatus(eventId: String, status: EventStatus) =
        mutate(eventId, "Đã cập nhật trạng thái sự kiện") { events.setStatus(eventId, status) }

    private fun mutate(
        eventId: String,
        successMessage: String = "Đã cập nhật đăng ký",
        operation: suspend () -> Result<Unit>,
    ) {
        if (_state.value.busyEvent != null) return
        _state.update { it.copy(busyEvent = eventId, error = null, message = null) }
        viewModelScope.launch {
            try {
                operation().getOrThrow()
                _state.update { it.copy(message = successMessage) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _state.update { it.copy(error = error.message ?: "Thao tác thất bại") }
            } finally {
                _state.update { it.copy(busyEvent = null) }
            }
        }
    }

    fun checkIn(token: String) {
        if (_state.value.checkingIn) return
        _state.update { it.copy(checkingIn = true, message = null, error = null) }
        viewModelScope.launch {
            try {
                tickets.checkIn(token).getOrThrow()
                _state.update { it.copy(message = "Điểm danh thành công") }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _state.update { it.copy(error = error.message ?: "Không thể điểm danh") }
            } finally {
                _state.update { it.copy(checkingIn = false) }
            }
        }
    }

    fun showError(message: String) { _state.update { it.copy(error = message, message = null) } }
}
