package com.giathinh.uthconnect.presentation.utilities

import com.giathinh.uthconnect.domain.model.AppSettings
import com.giathinh.uthconnect.domain.model.DeliveryPhase
import com.giathinh.uthconnect.domain.model.LANGUAGE_TAG_ENGLISH
import com.giathinh.uthconnect.domain.model.LANGUAGE_TAG_VIETNAMESE
import com.giathinh.uthconnect.domain.model.ThemeMode
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull
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
    ): UtilitiesViewModel = UtilitiesViewModel(
        getUtilityModules = GetUtilityModulesUseCase(utilityRepo),
        getTuition = GetTuitionUseCase(tuitionRepo),
        getTrainingPoint = GetTrainingPointUseCase(trainingPointRepo),
        getSettings = GetSettingsUseCase(settingsRepo),
        updateSettings = UpdateSettingsUseCase(settingsRepo),
    )

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
    fun `TuitionReminderChanged cap nhat settings thanh cong`() = runTest {
        val settingsRepo = FakeSettingsRepository()
        val viewModel = createViewModel(settingsRepo = settingsRepo)

        viewModel.onAction(UtilitiesAction.TuitionReminderChanged(false))
        assertFalse(settingsRepo.currentSettings.tuitionReminderEnabled)

        viewModel.onAction(UtilitiesAction.TuitionReminderChanged(true))
        assertTrue(settingsRepo.currentSettings.tuitionReminderEnabled)
    }

    @Test
    fun `LanguageSelected luu tag va phat event de Route dung lai activity`() = runTest {
        val settingsRepo = FakeSettingsRepository()
        val viewModel = createViewModel(settingsRepo = settingsRepo)

        viewModel.onAction(UtilitiesAction.LanguageSelected(LANGUAGE_TAG_ENGLISH))

        assertEquals(LANGUAGE_TAG_ENGLISH, settingsRepo.currentSettings.languageTag)
        assertEquals(
            "phai phat event de `UtilitiesRoute` goi recreate() sau khi store da ghi",
            UtilitiesEvent.LanguageChanged,
            viewModel.events.first(),
        )
    }

    @Test
    fun `chon lai dung ngon ngu dang bat - khong phat event vi recreate thua lam trang man hinh`() = runTest {
        val settingsRepo = FakeSettingsRepository()
        val viewModel = createViewModel(settingsRepo = settingsRepo)

        viewModel.onAction(UtilitiesAction.LanguageSelected(LANGUAGE_TAG_VIETNAMESE))

        assertNull("khong duoc phat event", withTimeoutOrNull(1_000) { viewModel.events.first() })
    }

    @Test
    fun `ThemeModeSelected luu vao store va phoi ra qua settingsState`() = runTest {
        val settingsRepo = FakeSettingsRepository()
        val viewModel = createViewModel(settingsRepo = settingsRepo)
        // settingsState dung stateIn(WhileSubscribed) nen phai co nguoi thu de upstream chay.
        backgroundScope.launch { viewModel.settingsState.collect { } }
        advanceUntilIdle()

        viewModel.onAction(UtilitiesAction.ThemeModeSelected(ThemeMode.DARK))
        advanceUntilIdle()

        assertEquals(ThemeMode.DARK, settingsRepo.currentSettings.themeMode)
        val shown = viewModel.settingsState.value.themeMode
        assertEquals("SettingsUiState phai phoi dung giao dien da chon", ThemeMode.DARK, shown)
    }

    private class FakeUtilityRepository(
        private val modules: List<UtilityModule>,
        var failed: Boolean = false,
    ) : UtilityModuleRepository {
        override suspend fun modules(): Result<List<UtilityModule>> = if (failed) Result.failure(IllegalStateException("mat mang")) else Result.success(modules)
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

        override fun currentLanguageTag(): String = currentSettings.languageTag

        override suspend fun updateTuitionReminder(enabled: Boolean) {
            currentSettings = currentSettings.copy(tuitionReminderEnabled = enabled)
            flow.value = currentSettings
        }

        override suspend fun updateTrainingPointNotif(enabled: Boolean) {
            currentSettings = currentSettings.copy(trainingPointNotifEnabled = enabled)
            flow.value = currentSettings
        }

        override suspend fun setLanguageTag(languageTag: String) {
            currentSettings = currentSettings.copy(languageTag = languageTag)
            flow.value = currentSettings
        }

        override suspend fun setThemeMode(themeMode: ThemeMode) {
            currentSettings = currentSettings.copy(themeMode = themeMode)
            flow.value = currentSettings
        }
    }
}
