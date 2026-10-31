package com.giathinh.uthconnect.presentation.tickets

import com.giathinh.uthconnect.domain.model.Ticket
import com.giathinh.uthconnect.domain.model.TicketStatus
import com.giathinh.uthconnect.domain.model.StudentProfile
import com.giathinh.uthconnect.domain.repository.SessionRepository
import com.giathinh.uthconnect.domain.repository.StudentSession
import com.giathinh.uthconnect.domain.repository.TicketRepository
import com.giathinh.uthconnect.domain.usecase.GetProfileUseCase
import com.giathinh.uthconnect.domain.usecase.GetTicketsUseCase
import com.giathinh.uthconnect.testing.MainDispatcherRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class TicketsViewModelTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()
    private val tickets = listOf(
        Ticket("t1", STUDENT_ID, "e1", "Hội thao", "Quang Trung", "12/11/2026", TicketStatus.VALID, "123 456"),
        Ticket("t2", STUDENT_ID, "e2", "Giao lưu CLB", "Nguyễn Tất Thành", "15/11/2026", TicketStatus.CHECKED_IN, "654 321"),
    )

    @Test
    fun `repository thành công - nạp hai vé và tắt loading`() {
        val repository = FakeTicketRepository(Result.success(tickets))
        val viewModel = viewModel(repository)
        assertEquals(tickets, viewModel.uiState.value.tickets)
        assertFalse(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.failure)
        assertEquals(STUDENT_ID, repository.requestedStudentId)
    }

    @Test
    fun `repository lỗi - state báo LoadFailed`() {
        val viewModel = viewModel(FakeTicketRepository(Result.failure(IllegalStateException("mất mạng"))))
        assertEquals(TicketsFailure.LoadFailed, viewModel.uiState.value.failure)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `repository trả danh sách rỗng - state empty khác với lỗi`() {
        val viewModel = viewModel(FakeTicketRepository(Result.success(emptyList())))
        assertTrue(viewModel.uiState.value.isEmpty)
        assertNull(viewModel.uiState.value.failure)
    }

    @Test
    fun `mở vé - chọn vé, phát điều hướng QR và bắt đầu countdown`() = runBlocking {
        val viewModel = viewModel(FakeTicketRepository(Result.success(tickets)))
        viewModel.onAction(TicketsAction.OpenTicket("t2"))
        assertEquals(tickets[1], viewModel.uiState.value.selectedTicket)
        assertEquals(60, viewModel.uiState.value.secondsRemaining)
        assertEquals(TicketsEvent.OpenQr("t2"), viewModel.events.first())
        viewModel.onAction(TicketsAction.CloseTicket)
        assertNull(viewModel.uiState.value.selectedTicket)
    }

    @Test
    fun `UiState khởi tạo loading thay vì rỗng`() {
        val state = TicketsUiState()
        assertTrue(state.isLoading)
        assertFalse(state.isEmpty)
    }

    @Test
    fun `nhóm vé suy ra từ trạng thái - vé còn hiệu lực thuộc Sắp diễn ra`() {
        val state = TicketsUiState(isLoading = false, tickets = tickets)
        assertEquals(listOf(tickets[0]), state.upcomingTickets)
        assertEquals(listOf(tickets[1]), state.pastTickets)
        assertEquals(listOf(tickets[0]), state.visibleTickets)
    }

    @Test
    fun `chọn tab Đã qua - danh sách hiển thị chỉ còn vé đã dùng`() {
        val viewModel = viewModel(FakeTicketRepository(Result.success(tickets)))
        viewModel.onAction(TicketsAction.SelectFilter(TicketFilter.Past))
        assertEquals(TicketFilter.Past, viewModel.uiState.value.filter)
        assertEquals(listOf(tickets[1]), viewModel.uiState.value.visibleTickets)
    }

    @Test
    fun `hồ sơ đã đăng nhập - tên sinh viên vào state để màn chi tiết vé in kèm MSSV`() {
        val viewModel = viewModel(FakeTicketRepository(Result.success(tickets)))
        assertEquals("Sinh viên", viewModel.uiState.value.studentName)
    }

    private fun viewModel(repository: FakeTicketRepository) = TicketsViewModel(
        GetTicketsUseCase(repository),
        GetProfileUseCase(FakeSessionRepository()),
    )

    private class FakeTicketRepository(private val result: Result<List<Ticket>>) : TicketRepository {
        var requestedStudentId: String? = null
        override suspend fun getTickets(studentId: String): Result<List<Ticket>> {
            requestedStudentId = studentId
            return result
        }
    }

    private class FakeSessionRepository : SessionRepository {
        override val session: StateFlow<StudentSession> = MutableStateFlow(
            StudentSession(isRestoring = false, profile = StudentProfile(STUDENT_ID, "s@uth.edu.vn", "Sinh viên", "CN23A", "CNTT", "CNTT")),
        )
        override suspend fun restore() = Unit
        override suspend fun start(profile: StudentProfile, remember: Boolean) = Unit
        override suspend fun signOut() = Unit
    }

    private companion object { const val STUDENT_ID = "2023040123" }
}
