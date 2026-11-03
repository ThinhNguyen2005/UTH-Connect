package com.giathinh.uthconnect.presentation.utilities

import com.giathinh.uthconnect.domain.model.AppSettings
import com.giathinh.uthconnect.domain.model.DeliveryPhase
import com.giathinh.uthconnect.domain.model.TrainingCriteria
import com.giathinh.uthconnect.domain.model.TrainingPointInfo
import com.giathinh.uthconnect.domain.model.TuitionCourse
import com.giathinh.uthconnect.domain.model.TuitionInfo
import com.giathinh.uthconnect.domain.model.UtilityModule
import com.giathinh.uthconnect.domain.model.UtilityModuleId
import com.giathinh.uthconnect.domain.repository.SettingsRepository
import com.giathinh.uthconnect.domain.repository.TrainingPointRepository
import com.giathinh.uthconnect.domain.repository.TuitionRepository
import com.giathinh.uthconnect.domain.repository.UtilityModuleRepository
import com.giathinh.uthconnect.domain.usecase.GetSettingsUseCase
import com.giathinh.uthconnect.domain.usecase.GetTrainingPointUseCase
import com.giathinh.uthconnect.domain.usecase.GetTuitionUseCase
import com.giathinh.uthconnect.domain.usecase.GetUtilityModulesUseCase
import com.giathinh.uthconnect.domain.usecase.UpdateSettingsUseCase
import com.giathinh.uthconnect.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class UtilitiesViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val modules = listOf(
        UtilityModule(UtilityModuleId.EVENTS, DeliveryPhase.MVP, enabled = true),
        UtilityModule(UtilityModuleId.TIMETABLE, DeliveryPhase.PHASE_2, enabled = false),
    )

    private fun createViewModel(
        utilityRepo: UtilityModuleRepository = FakeUtilityRepository(modules),
        tuitionRepo: TuitionRepository = FakeTuitionRepository(),
        trainingPointRepo: TrainingPointRepository = FakeTrainingPointRepository(),
        settingsRepo: SettingsRepository = FakeSettingsRepository(),
    ): UtilitiesViewModel {
        return UtilitiesViewModel(
            getUtilityModules = GetUtilityModulesUseCase(utilityRepo),
            getTuition = GetTuitionUseCase(tuitionRepo),
            getTrainingPoint = GetTrainingPointUseCase(trainingPointRepo),
            getSettings = GetSettingsUseCase(settingsRepo),
            updateSettings = UpdateSettingsUseCase(settingsRepo),
        )
    }

    @Test
    fun `mo tab - danh sach don tu repository va loading tat di`() {
        val viewModel = createViewModel()

        val state = viewModel.uiState.value
        assertEquals(modules, state.modules)
        assertFalse("da tai xong thi isLoading phai tat", state.isLoading)
        assertNull(state.failure)
    }

    @Test
    fun `repository that bai - UiState bao LoadFailed, khong giu danh sach rong`() {
        val viewModel = createViewModel(utilityRepo = FakeUtilityRepository(modules, failed = true))

        val state = viewModel.uiState.value
        assertEquals(UtilitiesFailure.LoadFailed, state.failure)
        assertTrue(state.modules.isEmpty())
        assertFalse(state.isLoading)
    }

    @Test
    fun `action Retry - ViewModel nap lai va xoa failure`() {
        val repository = FakeUtilityRepository(modules, failed = true)
        val viewModel = createViewModel(utilityRepo = repository)
        assertEquals(UtilitiesFailure.LoadFailed, viewModel.uiState.value.failure)

        repository.failed = false
        viewModel.onAction(UtilitiesAction.Retry)

        val state = viewModel.uiState.value
        assertEquals(modules, state.modules)
        assertNull("Retry thanh cong thi failure phai bi xoa", state.failure)
    }

    @Test
    fun `UiState mac dinh la loading, khong phai empty`() {
        val initial = UtilitiesUiState()

        assertTrue(initial.isLoading)
        assertFalse("dang tai thi chua duoc coi la rong", initial.isEmpty)
    }

    @Test
    fun `onToggleTuitionReminder cap nhat settings thanh cong`() = runTest {
        val settingsRepo = FakeSettingsRepository()
        val viewModel = createViewModel(settingsRepo = settingsRepo)

        viewModel.onToggleTuitionReminder(false)
        assertFalse(settingsRepo.currentSettings.tuitionReminderEnabled)

        viewModel.onToggleTuitionReminder(true)
        assertTrue(settingsRepo.currentSettings.tuitionReminderEnabled)
    }

    private class FakeUtilityRepository(
        private val modules: List<UtilityModule>,
        var failed: Boolean = false,
    ) : UtilityModuleRepository {
        override suspend fun modules(): Result<List<UtilityModule>> =
            if (failed) Result.failure(IllegalStateException("mat mang")) else Result.success(modules)
    }

    private class FakeTuitionRepository : TuitionRepository {
        override suspend fun getTuition(): Result<TuitionInfo> = Result.success(
            TuitionInfo(
                semester = "HK1",
                isPaid = true,
                totalAmount = "10.000.000 VNĐ",
                paidAmount = "10.000.000 VNĐ",
                remainingAmount = "0 VNĐ",
                creditsCount = 15,
                dueDate = "15/11/2025",
                courses = listOf(TuitionCourse("IT01", "Mobile", 3, "2.000.000 VNĐ")),
            ),
        )
    }

    private class FakeTrainingPointRepository : TrainingPointRepository {
        override suspend fun getTrainingPoint(): Result<TrainingPointInfo> = Result.success(
            TrainingPointInfo(
                semester = "HK1",
                totalScore = 90,
                maxScore = 100,
                rank = "XUẤT SẮC",
                criteriaList = listOf(TrainingCriteria("Học tập", 20, 20, "Tốt")),
            ),
        )
    }

    private class FakeSettingsRepository : SettingsRepository {
        var currentSettings = AppSettings()
        private val flow = MutableStateFlow(currentSettings)

        override fun getSettings(): Flow<AppSettings> = flow.asStateFlow()

        override suspend fun updateTuitionReminder(enabled: Boolean) {
            currentSettings = currentSettings.copy(tuitionReminderEnabled = enabled)
            flow.value = currentSettings
        }

        override suspend fun updateTrainingPointNotif(enabled: Boolean) {
            currentSettings = currentSettings.copy(trainingPointNotifEnabled = enabled)
            flow.value = currentSettings
        }
    }
}
