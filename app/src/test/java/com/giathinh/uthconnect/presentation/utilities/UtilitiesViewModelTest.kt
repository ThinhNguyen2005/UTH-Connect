package com.giathinh.uthconnect.presentation.utilities

import com.giathinh.uthconnect.domain.model.DeliveryPhase
import com.giathinh.uthconnect.domain.model.UtilityModule
import com.giathinh.uthconnect.domain.model.UtilityModuleId
import com.giathinh.uthconnect.domain.repository.UtilityModuleRepository
import com.giathinh.uthconnect.domain.usecase.GetUtilityModulesUseCase
import com.giathinh.uthconnect.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Test dọc theo khung MVVM: ViewModel + UseCase + Repository giả.
 * Đây là mẫu để các module khác copy — không cần mockito, không cần Robolectric.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class UtilitiesViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val modules = listOf(
        UtilityModule(UtilityModuleId.EVENTS, DeliveryPhase.MVP, enabled = true),
        UtilityModule(UtilityModuleId.TIMETABLE, DeliveryPhase.PHASE_2, enabled = false),
    )

    @Test
    fun `mở tab - danh sách đến từ repository và loading tắt đi`() {
        val viewModel = UtilitiesViewModel(GetUtilityModulesUseCase(FakeRepository(modules)))

        val state = viewModel.uiState.value
        assertEquals(modules, state.modules)
        assertFalse("đã tải xong thì isLoading phải tắt", state.isLoading)
        assertNull(state.failure)
    }

    @Test
    fun `repository thất bại - UiState báo LoadFailed, không giả danh danh sách rỗng`() {
        val viewModel = UtilitiesViewModel(GetUtilityModulesUseCase(FakeRepository(modules, failed = true)))

        val state = viewModel.uiState.value
        assertEquals(UtilitiesFailure.LoadFailed, state.failure)
        assertTrue(state.modules.isEmpty())
        assertFalse(state.isLoading)
    }

    @Test
    fun `action Retry - ViewModel nạp lại và xoá failure`() {
        val repository = FakeRepository(modules, failed = true)
        val viewModel = UtilitiesViewModel(GetUtilityModulesUseCase(repository))
        assertEquals(UtilitiesFailure.LoadFailed, viewModel.uiState.value.failure)

        repository.failed = false
        viewModel.onAction(UtilitiesAction.Retry)

        val state = viewModel.uiState.value
        assertEquals(modules, state.modules)
        assertNull("Retry thành công thì failure phải bị xoá", state.failure)
    }

    /**
     * Trạng thái khởi đầu phải là loading chứ **không** phải empty — nếu đổi lại,
     * UI sẽ nháy "Chưa có module nào" trước khi dữ liệu về. Test chốt hợp đồng đó.
     */
    @Test
    fun `UiState mặc định là loading, không phải empty`() {
        val initial = UtilitiesUiState()

        assertTrue(initial.isLoading)
        assertFalse("đang tải thì chưa được coi là rỗng", initial.isEmpty)
    }

    private class FakeRepository(
        private val modules: List<UtilityModule>,
        var failed: Boolean = false,
    ) : UtilityModuleRepository {
        override suspend fun modules(): Result<List<UtilityModule>> =
            if (failed) Result.failure(IllegalStateException("mất mạng")) else Result.success(modules)
    }
}
