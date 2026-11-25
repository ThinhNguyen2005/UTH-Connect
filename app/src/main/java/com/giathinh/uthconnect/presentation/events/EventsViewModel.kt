package com.giathinh.uthconnect.presentation.events

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.giathinh.uthconnect.domain.model.Event
import com.giathinh.uthconnect.domain.model.EventComment
import com.giathinh.uthconnect.domain.model.EventDraft
import com.giathinh.uthconnect.domain.model.EventStatus
import com.giathinh.uthconnect.domain.model.ServerErrorCode
import com.giathinh.uthconnect.domain.model.StudentProfile
import com.giathinh.uthconnect.domain.model.UserRole
import com.giathinh.uthconnect.domain.repository.EventRepository
import com.giathinh.uthconnect.domain.repository.TicketQrCheckInStatus
import com.giathinh.uthconnect.domain.repository.TicketRepository
import com.giathinh.uthconnect.domain.usecase.GetProfileUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class EventsUiState(
    val events: List<Event> = emptyList(),
    val registeredIds: Set<String> = emptySet(),
    val role: UserRole = UserRole.STUDENT,
    val currentUserId: String? = null,
    val loading: Boolean = true,
    val busyEvent: String? = null,
    val checkingIn: Boolean = false,
    val checkedInCount: Int = 0,
    val message: String? = null,
    val checkInSucceeded: Boolean = false,
    val checkInAlreadyAt: String? = null,
    val checkInError: String? = null,
    val checkInErrorCode: ServerErrorCode? = null,
    /**
     * Chuỗi lỗi thô từ tầng data. Để debug và làm fallback khi `errorCode == Unknown`;
     * giao diện luôn ưu tiên `errorCode?.messageText(error)`.
     */
    val error: String? = null,
    /** Mã lỗi server nhận diện được để màn hình dịch ra câu có người đọc được. */
    val errorCode: ServerErrorCode? = null,
    val activeDiscussionEvent: Event? = null,
    val commentsByEvent: Map<String, List<EventComment>> = emptyMap(),
    val commentDraft: String = "",
    val isPostingComment: Boolean = false,
    val selectedGuestProfile: StudentProfile? = null,
    val isLoadingGuestProfile: Boolean = false,
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
        _state.update { it.copy(loading = true, error = null, errorCode = null) }
        eventJob = viewModelScope.launch {
            events.getEvents().catch { error ->
                if (error is CancellationException) throw error
                _state.update {
                    it.copy(
                        loading = false,
                        error = error.message ?: "Không thể tải sự kiện",
                        errorCode = ServerErrorCode.of(error),
                    )
                }
            }.collect { result ->
                _state.update { it.copy(loading = false, events = result, error = null, errorCode = null) }
            }
        }
        registrationJob = viewModelScope.launch {
            events.currentUserRegistrations().catch { error ->
                if (error is CancellationException) throw error
                _state.update {
                    it.copy(
                        error = error.message ?: "Không thể tải đăng ký",
                        errorCode = ServerErrorCode.of(error),
                    )
                }
            }.collect { ids -> _state.update { it.copy(registeredIds = ids) } }
        }
        events.refresh()
    }

    fun register(eventId: String) = mutate(eventId) { events.registerCurrentUser(eventId) }

    fun cancel(eventId: String) = mutate(eventId) { events.cancelCurrentRegistration(eventId) }

    fun saveEvent(eventId: String?, draft: EventDraft) = mutate(eventId ?: "new", "Đã lưu sự kiện") { events.saveEvent(eventId, draft) }

    fun setStatus(eventId: String, status: EventStatus) = mutate(eventId, "Đã cập nhật trạng thái sự kiện") { events.setStatus(eventId, status) }

    private fun mutate(
        eventId: String,
        successMessage: String = "Đã cập nhật đăng ký",
        operation: suspend () -> Result<Unit>,
    ) {
        if (_state.value.busyEvent != null) return
        _state.update { it.copy(busyEvent = eventId, error = null, errorCode = null, message = null) }
        viewModelScope.launch {
            try {
                operation().getOrThrow()
                _state.update { it.copy(message = successMessage) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _state.update {
                    it.copy(
                        error = error.message ?: "Thao tác thất bại",
                        errorCode = ServerErrorCode.of(error),
                    )
                }
            } finally {
                _state.update { it.copy(busyEvent = null) }
            }
        }
    }

    fun checkIn(token: String) {
        if (_state.value.checkingIn) return
        _state.update {
            it.copy(
                checkingIn = true,
                checkInSucceeded = false,
                checkInAlreadyAt = null,
                checkInError = null,
                checkInErrorCode = null,
            )
        }
        viewModelScope.launch {
            try {
                val outcome = tickets.checkInTicketQr(token).getOrThrow()
                when (outcome.status) {
                    TicketQrCheckInStatus.CHECKED_IN -> _state.update {
                        it.copy(checkInSucceeded = true, checkedInCount = it.checkedInCount + 1)
                    }

                    TicketQrCheckInStatus.ALREADY_CHECKED_IN -> _state.update {
                        it.copy(checkInAlreadyAt = formatCheckInTime(outcome.checkedInAt))
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _state.update {
                    it.copy(
                        checkInError = error.message ?: "Không thể điểm danh",
                        checkInErrorCode = ServerErrorCode.of(error),
                    )
                }
            } finally {
                _state.update { it.copy(checkingIn = false) }
            }
        }
    }

    /**
     * Xoá kết quả một lượt quét để máy quét về trạng thái chờ mã tiếp theo. Màn quét
     * suy ra `QrScannerOutcome` từ `message`/`error` nên bắt buộc phải có lệnh này.
     */
    fun clearCheckInResult() {
        _state.update {
            it.copy(
                checkInSucceeded = false,
                checkInAlreadyAt = null,
                checkInError = null,
                checkInErrorCode = null,
            )
        }
    }

    fun onDiscussionAction(action: EventDiscussionAction) {
        when (action) {
            is EventDiscussionAction.Open -> {
                _state.update { it.copy(activeDiscussionEvent = action.event, commentDraft = "") }
                viewModelScope.launch {
                    events.getEventComments(action.event.id).collect { comments ->
                        _state.update { state ->
                            state.copy(commentsByEvent = state.commentsByEvent + (action.event.id to comments))
                        }
                    }
                }
            }

            EventDiscussionAction.Close -> {
                _state.update { it.copy(activeDiscussionEvent = null, commentDraft = "") }
            }

            is EventDiscussionAction.DraftChange -> {
                _state.update { it.copy(commentDraft = action.text) }
            }

            is EventDiscussionAction.Send -> postComment(action.eventId)

            is EventDiscussionAction.OpenGuestProfile -> {
                if (action.studentId.isNotBlank()) {
                    _state.update { it.copy(isLoadingGuestProfile = true) }
                    viewModelScope.launch {
                        try {
                            val profile = events.getGuestProfile(action.studentId).getOrNull()
                            _state.update { it.copy(selectedGuestProfile = profile, isLoadingGuestProfile = false) }
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } catch (_: Exception) {
                            _state.update { it.copy(isLoadingGuestProfile = false) }
                        }
                    }
                }
            }

            EventDiscussionAction.CloseGuestProfile -> {
                _state.update { it.copy(selectedGuestProfile = null) }
            }
        }
    }

    private fun postComment(eventId: String) {
        val draft = _state.value.commentDraft.trim()
        if (draft.isBlank() || _state.value.isPostingComment) return
        _state.update { it.copy(isPostingComment = true) }
        viewModelScope.launch {
            try {
                val comment = events.addEventComment(eventId, draft).getOrThrow()
                _state.update { state ->
                    val existing = state.commentsByEvent[eventId].orEmpty()
                    state.copy(
                        isPostingComment = false,
                        commentDraft = "",
                        commentsByEvent = state.commentsByEvent + (eventId to (existing + comment)),
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _state.update { it.copy(isPostingComment = false) }
            }
        }
    }
}

internal fun formatCheckInTime(value: String): String = OffsetDateTime.parse(value)
    .toInstant()
    .atZone(ZoneId.of("Asia/Ho_Chi_Minh"))
    .format(CHECK_IN_TIME_FORMAT)

private val CHECK_IN_TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm dd/MM")
