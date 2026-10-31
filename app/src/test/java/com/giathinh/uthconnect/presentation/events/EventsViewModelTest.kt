package com.giathinh.uthconnect.presentation.events

import com.giathinh.uthconnect.data.repository.StaticEventRepository
import com.giathinh.uthconnect.domain.model.Campus
import com.giathinh.uthconnect.domain.model.Event
import com.giathinh.uthconnect.domain.repository.EventRepository
import com.giathinh.uthconnect.domain.usecase.GetEventsUseCase
import com.giathinh.uthconnect.testing.MainDispatcherRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EventsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `mo man hinh tai du 6 su kien`() {
        val viewModel = EventsViewModel(GetEventsUseCase(StaticEventRepository()))

        assertEquals(6, viewModel.uiState.value.events.size)
        assertFalse(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.failure)
    }

    @Test
    fun `chon chip loc dung co so va tat ca khoi phuc 6 su kien`() {
        val viewModel = EventsViewModel(GetEventsUseCase(StaticEventRepository()))

        viewModel.onAction(EventsAction.SelectCampus(Campus.THU_DUC))
        assertEquals(Campus.THU_DUC, viewModel.uiState.value.selectedCampus)
        assertEquals(2, viewModel.uiState.value.events.size)
        assertTrue(viewModel.uiState.value.events.all { it.campus == Campus.THU_DUC })

        viewModel.onAction(EventsAction.SelectCampus(null))
        assertEquals(6, viewModel.uiState.value.events.size)
        assertNull(viewModel.uiState.value.selectedCampus)
    }

    @Test
    fun `tai that bai hien loi chu khong hien danh sach rong`() {
        val repository = FakeRepository(failed = true)
        val viewModel = EventsViewModel(GetEventsUseCase(repository))

        assertEquals(EventsFailure.LoadFailed, viewModel.uiState.value.failure)
        assertFalse(viewModel.uiState.value.isLoading)
        assertFalse(viewModel.uiState.value.isEmpty)
        assertTrue(viewModel.uiState.value.events.isEmpty())
    }

    @Test
    fun `Retry xoa loi va giu nguyen bo loc`() {
        val repository = FakeRepository(failed = true)
        val viewModel = EventsViewModel(GetEventsUseCase(repository))
        viewModel.onAction(EventsAction.SelectCampus(Campus.QUAN_12))

        repository.failed = false
        viewModel.onAction(EventsAction.Retry)

        assertNull(viewModel.uiState.value.failure)
        assertEquals(Campus.QUAN_12, viewModel.uiState.value.selectedCampus)
        assertEquals(2, viewModel.uiState.value.events.size)
        assertTrue(viewModel.uiState.value.events.all { it.campus == Campus.QUAN_12 })
    }

    @Test
    fun `repository rong la empty thanh cong`() {
        val repository = object : EventRepository {
            override suspend fun events() = Result.success(emptyList<Event>())
        }
        val viewModel = EventsViewModel(GetEventsUseCase(repository))

        assertTrue(viewModel.uiState.value.isEmpty)
        assertNull(viewModel.uiState.value.failure)
    }

    @Test
    fun `chon chip moi huy tai cu dang cho`() {
        val oldRequest = CompletableDeferred<Unit>()
        val newRequest = CompletableDeferred<Unit>()
        var calls = 0
        val repository = object : EventRepository {
            override suspend fun events(): Result<List<Event>> {
                calls++
                if (calls == 2) oldRequest.await()
                if (calls == 3) newRequest.await()
                return StaticEventRepository().events()
            }
        }
        val viewModel = EventsViewModel(GetEventsUseCase(repository))
        viewModel.onAction(EventsAction.SelectCampus(Campus.THU_DUC))
        assertTrue(viewModel.uiState.value.isLoading)
        viewModel.onAction(EventsAction.SelectCampus(Campus.BINH_THANH))
        newRequest.complete(Unit)
        oldRequest.complete(Unit)

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(Campus.BINH_THANH, viewModel.uiState.value.selectedCampus)
        assertEquals(2, viewModel.uiState.value.events.size)
        assertTrue(viewModel.uiState.value.events.all { it.campus == Campus.BINH_THANH })
    }

    @Test
    fun `trang thai ban dau la loading khong phai empty`() {
        assertTrue(EventsUiState().isLoading)
        assertFalse(EventsUiState().isEmpty)
    }

    /** Fake nhỏ, đổi cờ failed để kiểm tra nhánh lỗi và nút thử lại. */
    private class FakeRepository(var failed: Boolean) : EventRepository {
        override suspend fun events(): Result<List<Event>> =
            if (failed) Result.failure(IllegalStateException("Không đọc được dữ liệu"))
            else StaticEventRepository().events()
    }
}
